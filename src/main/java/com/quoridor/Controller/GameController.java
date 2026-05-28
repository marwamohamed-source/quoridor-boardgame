package com.quoridor.controller;

import com.quoridor.ai.AIPlayer;
import com.quoridor.model.*;
import com.quoridor.view.GameView;
import javafx.application.Platform;

import java.util.List;

/**
 * GameController bridges the View and the Model.
 *
 * It is the only class allowed to call state-mutating methods
 * on GameState (movePawn, placeWall).  The View never touches
 * the model directly; the Model never touches the View.
 *
 * Flow for each turn:
 *  1. beginTurn()  — highlight legal moves, enable input
 *  2. User clicks  — handleCellClick() or handleWallClick()
 *  3. Validate     — ask MoveValidator
 *  4. Apply        — call GameState.movePawn() or placeWall()
 *  5. Refresh View — gameView.refresh()
 *  6. Check win    — if game over, show result; else beginTurn()
 */
public class GameController {

    // ── Mode enum ─────────────────────────────────────────────
    public enum InputMode { PAWN, WALL }

    // ── Dependencies ──────────────────────────────────────────
    private GameView  gameView;
    private GameState gameState;
    private AIPlayer  aiPlayer;     // null in Human vs Human mode

    // ── State ─────────────────────────────────────────────────
    private InputMode inputMode   = InputMode.PAWN;
    private boolean   vsComputer  = false;
    private boolean   inputLocked = false; // locked during AI "thinking"

    // ── Constructor ───────────────────────────────────────────

    public GameController() {
        // Dependencies injected via setters after construction
    }

    // ── Dependency injection ──────────────────────────────────

    public void setGameView(GameView view) {
        this.gameView = view;
    }

    // ════════════════════════════════════════════════════════
    //  GAME LIFECYCLE
    // ════════════════════════════════════════════════════════

    /**
     * Starts a new game.
     * Called by GameView when the player picks a mode.
     *
     * @param vsComputer  true = Human vs AI, false = Human vs Human
     */
    public void startGame(boolean vsComputer) {
    this.vsComputer  = vsComputer;
    this.gameState   = new GameState(vsComputer);
    this.inputMode   = InputMode.PAWN;
    this.inputLocked = false;

    // Create AI with selected difficulty
    this.aiPlayer = vsComputer
        ? new AIPlayer(aiDifficulty) : null;

    gameView.showGameScreen(gameState);
    beginTurn();
}

    /** Resets to a fresh game with the same mode. */
    public void resetGame() {
        startGame(vsComputer);
    }

    // ════════════════════════════════════════════════════════
    //  TURN MANAGEMENT
    // ════════════════════════════════════════════════════════

    /**
     * Prepares the UI for the current player's turn:
     *  - Switches to PAWN mode by default
     *  - Highlights all legal pawn moves in green
     *  - If it's the AI's turn, triggers AI move after a short delay
     */
    private void beginTurn() {
        if (gameState.isGameOver()) return;

        setInputMode(InputMode.PAWN);

        // Highlight legal pawn moves for the human player
        boolean currentIsAI = vsComputer
            && gameState.getCurrentPlayerIndex() == 1;

        if (!currentIsAI) {
            List<Position> legalMoves =
                MoveValidator.getLegalPawnMoves(gameState);
            gameView.getBoardRenderer().highlightValidMoves(legalMoves);
            inputLocked = false;
        } else {
            // AI's turn — lock input and schedule AI move
            inputLocked = true;
            gameView.getBoardRenderer().clearHighlights();
            triggerAIMove();
        }
    }

    /**
     * Runs the AI move on a background thread so the UI
     * doesn't freeze during computation.
     */
    private void triggerAIMove() {
        gameView.showMessage("Computer is thinking...");

        Thread aiThread = new Thread(() -> {
            // Simulate a short "thinking" pause for UX
            try { Thread.sleep(600); } catch (InterruptedException ignored) {}

            // AI computes its move on the background thread
            AIPlayer.AIMove move = aiPlayer.chooseMove(gameState);

            // Apply the move back on the JavaFX Application Thread
            Platform.runLater(() -> applyAIMove(move));
        });
        aiThread.setDaemon(true);
        aiThread.start();
    }

    /** Applies an AI-chosen move to the game state. */
    private void applyAIMove(AIPlayer.AIMove move) {
        if (move.isPawnMove()) {
            gameState.movePawn(move.getTargetPosition());
        } else {
            gameState.placeWall(move.getWall());
        }

        gameView.refresh(gameState);
        afterMove();
    }

    /**
     * Called after every move (human or AI):
     *  - Checks for win condition
     *  - If game continues, begins the next turn
     */
    private void afterMove() {
        if (gameState.isGameOver()) {
            gameView.refresh(gameState);
            gameView.getBoardRenderer().clearHighlights();
            inputLocked = true;
            return;
        }
        beginTurn();
    }

    // ════════════════════════════════════════════════════════
    //  HUMAN INPUT HANDLERS
    // ════════════════════════════════════════════════════════

    /**
     * Called by BoardRenderer when the user clicks a cell.
     *
     * In PAWN mode:  attempt to move the pawn there.
     * In WALL mode:  ignore cell clicks.
     */
    public void handleCellClick(Position target) {
        if (inputLocked) return;
        if (inputMode != InputMode.PAWN) return;

        if (MoveValidator.isLegalPawnMove(gameState, target)) {
            gameState.movePawn(target);
            gameView.refresh(gameState);
            afterMove();
        } else {
            gameView.showMessage("Invalid move! Click a green cell.");
        }
    }

    /**
     * Called by BoardRenderer when the user clicks a wall slot.
     *
     * In WALL mode:  attempt to place the wall.
     * In PAWN mode:  switch to wall mode and preview.
     */
    public void handleWallClick(Wall wall) {
        if (inputLocked) return;

        // Auto-switch to wall mode on wall slot click
        if (inputMode == InputMode.PAWN) {
            setInputMode(InputMode.WALL);
        }

        if (gameState.getCurrentPlayer().getWallsRemaining() <= 0) {
            gameView.showMessage("No walls remaining!");
            return;
        }

        if (MoveValidator.isLegalWallPlacement(gameState, wall)) {
            gameState.placeWall(wall);
            gameView.refresh(gameState);
            afterMove();
        } else {
            gameView.showMessage(
                "Invalid wall placement! Would block a path or overlaps.");
        }
    }

    // ════════════════════════════════════════════════════════
    //  INPUT MODE MANAGEMENT
    // ════════════════════════════════════════════════════════

    /**
     * Switches between PAWN and WALL placement modes.
     * Updates the View to reflect the new mode.
     */
    public void setInputMode(InputMode mode) {
        this.inputMode = mode;

        if (mode == InputMode.PAWN) {
            // Show green valid move highlights
            List<Position> legalMoves =
                MoveValidator.getLegalPawnMoves(gameState);
            gameView.getBoardRenderer().highlightValidMoves(legalMoves);
            gameView.showMessage(
                gameState.getCurrentPlayer().getName()
                + "'s turn — click a green cell to move.");
        } else {
            // Clear pawn highlights; wall slots are always hoverable
            gameView.getBoardRenderer().clearHighlights();
            gameView.showMessage(
                gameState.getCurrentPlayer().getName()
                + " — click a wall slot to place a wall.");
        }

        // Tell the View to update the mode-toggle button label
        gameView.updateModeButton(mode);
    }

    public InputMode getInputMode() { return inputMode; }
    
    // Add this field at the top of GameController:
private AIPlayer.Difficulty aiDifficulty = AIPlayer.Difficulty.MEDIUM;

// Add this setter:
public void setAIDifficulty(AIPlayer.Difficulty difficulty) {
    this.aiDifficulty = difficulty;
    if (aiPlayer != null) aiPlayer.setDifficulty(difficulty);
}
}