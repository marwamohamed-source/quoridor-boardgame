package com.quoridor.view;

import com.quoridor.controller.GameController;
import com.quoridor.model.*;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * BoardRenderer builds and manages all visual nodes on the game board.
 *
 * The board is rendered as a Pane containing:
 *  - Cell rectangles  (9x9 = 81 nodes)
 *  - Horizontal wall slots  (8x9 gap rows)
 *  - Vertical wall slots    (9x8 gap cols)
 *  - Pawn circles           (2 nodes)
 *  - Placed wall rectangles (added dynamically)
 *  - Highlight overlays     (added/removed on each turn)
 *
 * Coordinate mapping:
 *   pixelX(col) = PADDING + col * (CELL_SIZE + GAP_SIZE)
 *   pixelY(row) = PADDING + row * (CELL_SIZE + GAP_SIZE)
 */
public class BoardRenderer {

    // ── Public constants (used by GameView for sizing) ────────
    public static final double CELL_SIZE  = 58;
    public static final double GAP_SIZE   = 12;
    public static final double PADDING    = 30;
    public static final double BOARD_PIXEL_SIZE =
        PADDING * 2 + 9 * CELL_SIZE + 8 * GAP_SIZE;  // ≈ 682px

    // Pawn colours — also used by GameView for player labels
    public static final String P1_COLOR = "#E84444";  // red
    public static final String P2_COLOR = "#4488EE";  // blue

    // ── Cell colour constants ─────────────────────────────────
    private static final Color COLOR_CELL_NORMAL   = Color.web("#F0DEB4");
    private static final Color COLOR_CELL_HOVER    = Color.web("#FFEAA0");
    private static final Color COLOR_CELL_VALID    = Color.web("#90EE90");
    private static final Color COLOR_CELL_DARK     = Color.web("#D4B896");
    private static final Color COLOR_WALL_SLOT     = Color.web("#5C3A1E");
    private static final Color COLOR_WALL_PLACED_H = Color.web("#8B4513");
    private static final Color COLOR_WALL_PLACED_V = Color.web("#8B4513");
    private static final Color COLOR_WALL_PREVIEW  = Color.web("#CD853F");
    private static final Color COLOR_BOARD_BG      = Color.web("#3D1F0A");

    // ── State ─────────────────────────────────────────────────
    private final Pane          boardPane;
    private GameController      controller;

    // Node grids for direct access
    private final Rectangle[][] cellNodes;   // [row][col]
    private final Rectangle[][] hSlotNodes;  // [r][c] — horizontal wall slots
    private final Rectangle[][] vSlotNodes;  // [r][c] — vertical wall slots

    // Pawn circles
    private Circle p1Pawn;
    private Circle p2Pawn;

    // Currently highlighted cells
    private final List<Position> highlightedCells = new ArrayList<>();

    // Wall placement mode flag
    private boolean wallPlacementMode = false;

    // ── Constructor ──────────────────────────────────────────

    public BoardRenderer(GameState initialState) {
        boardPane  = new Pane();
        boardPane.setPrefSize(BOARD_PIXEL_SIZE, BOARD_PIXEL_SIZE);
        boardPane.setStyle("-fx-background-color: #3D1F0A;");

        cellNodes  = new Rectangle[9][9];
        hSlotNodes = new Rectangle[8][9];
        vSlotNodes = new Rectangle[9][8];

        buildGrid();
        buildPawns(initialState);
        buildCoordinateLabels();
    }

    // ════════════════════════════════════════════════════════
    //  BOARD CONSTRUCTION
    // ════════════════════════════════════════════════════════

    /** Builds all cell rectangles and wall-slot rectangles. */
    private void buildGrid() {

        // ── Cells ──────────────────────────────────────────
        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < 9; col++) {
                double x = toPixelX(col);
                double y = toPixelY(row);

                Rectangle cell = new Rectangle(x, y, CELL_SIZE, CELL_SIZE);
                // Checkerboard shading for readability
                boolean dark = (row + col) % 2 == 1;
                cell.setFill(dark ? COLOR_CELL_DARK : COLOR_CELL_NORMAL);
                cell.setArcWidth(4);
                cell.setArcHeight(4);

                final int r = row, c = col;
                cell.setOnMouseEntered(e -> onCellHover(r, c));
                cell.setOnMouseExited(e  -> onCellExit(r, c));
                cell.setOnMouseClicked(e -> onCellClick(r, c));

                cellNodes[row][col] = cell;
                boardPane.getChildren().add(cell);
            }
        }

        // ── Horizontal wall slots (between rows) ────────────
        // There are 8 gap rows and 9 columns of slots
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 9; c++) {
                double x = toPixelX(c);
                double y = toPixelY(r) + CELL_SIZE;  // just below row r

                // Each slot is CELL_SIZE wide and GAP_SIZE tall
                Rectangle slot = new Rectangle(x, y, CELL_SIZE, GAP_SIZE);
                slot.setFill(COLOR_WALL_SLOT);
                slot.setOpacity(0.3);

                // For clicking: we care about anchor (r,c) and (r, c-1)
                final int fr = r, fc = c;
                slot.setOnMouseEntered(e ->
                    highlightWallPreview(fr, fc, Wall.Orientation.HORIZONTAL));
                slot.setOnMouseExited(e ->
                    clearWallPreview(fr, fc, Wall.Orientation.HORIZONTAL));
                slot.setOnMouseClicked(e ->
                    onWallSlotClick(fr, fc, Wall.Orientation.HORIZONTAL));

                hSlotNodes[r][c] = slot;
                boardPane.getChildren().add(slot);
            }
        }

        // ── Vertical wall slots (between columns) ───────────
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 8; c++) {
                double x = toPixelX(c) + CELL_SIZE;  // just right of col c
                double y = toPixelY(r);

                Rectangle slot = new Rectangle(x, y, GAP_SIZE, CELL_SIZE);
                slot.setFill(COLOR_WALL_SLOT);
                slot.setOpacity(0.3);

                final int fr = r, fc = c;
                slot.setOnMouseEntered(e ->
                    highlightWallPreview(fr, fc, Wall.Orientation.VERTICAL));
                slot.setOnMouseExited(e ->
                    clearWallPreview(fr, fc, Wall.Orientation.VERTICAL));
                slot.setOnMouseClicked(e ->
                    onWallSlotClick(fr, fc, Wall.Orientation.VERTICAL));

                vSlotNodes[r][c] = slot;
                boardPane.getChildren().add(slot);
            }
        }
    }

    /** Places coordinate labels (A-I columns, 1-9 rows) around the board. */
    private void buildCoordinateLabels() {
        String[] colLabels = {"A","B","C","D","E","F","G","H","I"};

        for (int c = 0; c < 9; c++) {
            // Bottom labels
            Text t = new Text(
                toPixelX(c) + CELL_SIZE / 2 - 5,
                BOARD_PIXEL_SIZE - 4,
                colLabels[c]
            );
            t.setFill(Color.web("#C4A882"));
            t.setFont(Font.font("Georgia", 11));
            boardPane.getChildren().add(t);
        }

        for (int r = 0; r < 9; r++) {
            // Left-side row numbers (9 at top, 1 at bottom — chess style)
            Text t = new Text(
                6,
                toPixelY(r) + CELL_SIZE / 2 + 5,
                String.valueOf(9 - r)
            );
            t.setFill(Color.web("#C4A882"));
            t.setFont(Font.font("Georgia", 11));
            boardPane.getChildren().add(t);
        }
    }

    /** Creates and places the two pawn circles on the board. */
    private void buildPawns(GameState state) {
        p1Pawn = createPawn(P1_COLOR);
        p2Pawn = createPawn(P2_COLOR);

        updatePawnPosition(p1Pawn, state.getPlayer1().getPosition());
        updatePawnPosition(p2Pawn, state.getPlayer2().getPosition());

        boardPane.getChildren().addAll(p1Pawn, p2Pawn);
    }

    /** Builds a styled pawn circle. */
    private Circle createPawn(String colorHex) {
        double radius = CELL_SIZE / 2 - 6;
        Circle pawn = new Circle(radius);
        pawn.setFill(Color.web(colorHex));
        pawn.setStroke(Color.WHITE);
        pawn.setStrokeWidth(2.5);
        pawn.setEffect(new javafx.scene.effect.DropShadow(
            6, Color.BLACK));
        pawn.setMouseTransparent(true); // clicks pass through to cells
        return pawn;
    }

    // ════════════════════════════════════════════════════════
    //  EVENT HANDLERS
    // ════════════════════════════════════════════════════════

    private void onCellHover(int row, int col) {
        Rectangle cell = cellNodes[row][col];
        // Only show hover if this cell is a valid move target
        if (highlightedCells.contains(new Position(row, col))) {
            cell.setFill(COLOR_CELL_HOVER);
        }
    }

    private void onCellExit(int row, int col) {
        // Restore highlight colour if it was a valid move target
        if (highlightedCells.contains(new Position(row, col))) {
            cellNodes[row][col].setFill(COLOR_CELL_VALID);
        } else {
            cellNodes[row][col].setFill(
                (row + col) % 2 == 1 ? COLOR_CELL_DARK : COLOR_CELL_NORMAL);
        }
    }

    private void onCellClick(int row, int col) {
        if (controller != null) {
            controller.handleCellClick(new Position(row, col));
        }
    }

    private void onWallSlotClick(int row, int col,
                                  Wall.Orientation orientation) {
        if (controller != null) {
            Wall wall = new Wall(orientation, new Position(row, col));
            controller.handleWallClick(wall);
        }
    }

    /** Brightens wall slots to preview a 2-slot wall span on hover. */
    private void highlightWallPreview(int row, int col,
                                       Wall.Orientation orientation) {
        setWallSlotHighlight(row, col, orientation, true);
    }

    private void clearWallPreview(int row, int col,
                                   Wall.Orientation orientation) {
        setWallSlotHighlight(row, col, orientation, false);
    }

    /**
     * Highlights or un-highlights the two wall slots that a wall
     * at (row, col) would occupy.
     */
    private void setWallSlotHighlight(int row, int col,
                                       Wall.Orientation orientation,
                                       boolean highlight) {
        if (orientation == Wall.Orientation.HORIZONTAL) {
            setHSlot(row, col,     highlight);
            setHSlot(row, col + 1, highlight);  // wall spans 2 columns
        } else {
            setVSlot(row,     col, highlight);
            setVSlot(row + 1, col, highlight);  // wall spans 2 rows
        }
    }

    private void setHSlot(int r, int c, boolean highlight) {
        if (r < 0 || r >= 8 || c < 0 || c >= 9) return;
        hSlotNodes[r][c].setOpacity(highlight ? 0.85 : 0.3);
        hSlotNodes[r][c].setFill(highlight
            ? COLOR_WALL_PREVIEW : COLOR_WALL_SLOT);
    }

    private void setVSlot(int r, int c, boolean highlight) {
        if (r < 0 || r >= 9 || c < 0 || c >= 8) return;
        vSlotNodes[r][c].setOpacity(highlight ? 0.85 : 0.3);
        vSlotNodes[r][c].setFill(highlight
            ? COLOR_WALL_PREVIEW : COLOR_WALL_SLOT);
    }

    // ════════════════════════════════════════════════════════
    //  HIGHLIGHT VALID MOVES
    // ════════════════════════════════════════════════════════

    /**
     * Highlights all cells in 'validMoves' with a green tint.
     * Clears any previous highlights first.
     *
     * Called by GameController at the start of each turn.
     */
    public void highlightValidMoves(List<Position> validMoves) {
        clearHighlights();
        highlightedCells.addAll(validMoves);
        for (Position p : validMoves) {
            cellNodes[p.row][p.col].setFill(COLOR_CELL_VALID);
        }
    }

    /** Removes all cell highlights. */
    public void clearHighlights() {
        for (Position p : highlightedCells) {
            cellNodes[p.row][p.col].setFill(
                (p.row + p.col) % 2 == 1
                    ? COLOR_CELL_DARK : COLOR_CELL_NORMAL);
        }
        highlightedCells.clear();
    }

    // ════════════════════════════════════════════════════════
    //  REDRAW (called after every move)
    // ════════════════════════════════════════════════════════

    /**
     * Full board refresh: repositions pawns, redraws all placed walls,
     * and refreshes valid-move highlights for the new current player.
     *
     * @param state  The updated game state to display.
     */
    public void redraw(GameState state) {
        // Update pawn positions
        updatePawnPosition(p1Pawn, state.getPlayer1().getPosition());
        updatePawnPosition(p2Pawn, state.getPlayer2().getPosition());

        // Redraw all walls
        redrawWalls(state.getBoard());
    }

    /** Moves a pawn circle to the pixel centre of the given board cell. */
    private void updatePawnPosition(Circle pawn, Position pos) {
        double cx = toPixelX(pos.col) + CELL_SIZE / 2;
        double cy = toPixelY(pos.row) + CELL_SIZE / 2;
        pawn.setCenterX(cx);
        pawn.setCenterY(cy);
    }

    /**
     * Redraws all placed walls by updating the wall slot rectangles.
     * Placed walls are shown as solid, opaque brown rectangles.
     */
    private void redrawWalls(Board board) {
        // Horizontal walls
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                if (board.isHWallAt(r, c)) {
                    // A horizontal wall at anchor (r,c) covers
                    // hSlotNodes[r][c] and hSlotNodes[r][c+1]
                    paintHWall(r, c);
                }
            }
        }
        // Vertical walls
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                if (board.isVWallAt(r, c)) {
                    paintVWall(r, c);
                }
            }
        }
    }

    /** Paints a placed horizontal wall at anchor (r, c). */
    private void paintHWall(int r, int c) {
        // Horizontal wall spans slot [r][c] and [r][c+1]
        for (int dc = 0; dc <= 1; dc++) {
            if (c + dc < 9) {
                hSlotNodes[r][c + dc].setFill(COLOR_WALL_PLACED_H);
                hSlotNodes[r][c + dc].setOpacity(1.0);
            }
        }
        // Also fill the intersection gap square between them
        // (the small square at the corner where cell corners meet)
        // This is handled automatically since adjacent slots overlap visually
    }

    /** Paints a placed vertical wall at anchor (r, c). */
    private void paintVWall(int r, int c) {
        // Vertical wall spans slot [r][c] and [r+1][c]
        for (int dr = 0; dr <= 1; dr++) {
            if (r + dr < 9) {
                vSlotNodes[r + dr][c].setFill(COLOR_WALL_PLACED_V);
                vSlotNodes[r + dr][c].setOpacity(1.0);
            }
        }
    }

    // ════════════════════════════════════════════════════════
    //  COORDINATE HELPERS
    // ════════════════════════════════════════════════════════

    /**
     * Converts a board column index to a pixel X coordinate.
     * Each column is CELL_SIZE + GAP_SIZE wide.
     */
    public static double toPixelX(int col) {
        return PADDING + col * (CELL_SIZE + GAP_SIZE);
    }

    /**
     * Converts a board row index to a pixel Y coordinate.
     */
    public static double toPixelY(int row) {
        return PADDING + row * (CELL_SIZE + GAP_SIZE);
    }

    // ── Getters ──────────────────────────────────────────────

    public Pane getBoardPane() { return boardPane; }

    public void setController(GameController controller) {
        this.controller = controller;
    }
}