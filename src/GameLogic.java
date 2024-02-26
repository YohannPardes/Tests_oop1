import java.util.*;

/**
 * The main class that control the logic and the game during the game
 */
public class GameLogic implements PlayableLogic {

    private final int board_size = 11;
    private ConcretePiece[][] board_data = new ConcretePiece[board_size][board_size];

    private ArrayList<MySet<ConcretePiece>> tile_history = new ArrayList<>();

    private final Player attacking_player = new ConcretePlayer("Black");
    private final Player defending_player = new ConcretePlayer("White");
    private boolean black_turn = true;
    private Stack<ConcretePiece[][]> move_history = new Stack<>();

    private ConcretePiece[] piece_list = new ConcretePiece[13 + 24];

    public GameLogic() {

        //preparing the tiles history
        for (int i = 0; i < board_size*board_size; i++) {
            this.tile_history.add(new MySet<>(new Position(i / board_size, i % board_size)));
        }

        // adding the white pieces
        this.SettingUpWhite();

        // adding the black pieces
        this.SettingUpBlack();
    }

    /**
     * using an array of x, y value4s assigning the black pieces accordingly
     */
    private void SettingUpBlack() {
        int[][] positions = {
                {0, 3}, {0, 4}, {0, 5}, {0, 6}, {0, 7},
                {1, 5},
                {3, 0}, {3, 10},
                {4, 0}, {4, 10},
                {5, 0}, {5, 1}, {5, 9}, {5, 10},
                {6, 0}, {6, 10},
                {7, 0}, {7, 10},
                {9, 5},
                {10, 3}, {10, 4}, {10, 5}, {10, 6}, {10, 7}
        };

        for (int i = 0; i < positions.length; i += 1) {
            int x = positions[i][1];
            int y = positions[i][0];
            ConcretePiece added_piece = new Pawn(this.attacking_player, i + 1, new Position(x, y));
            this.board_data[x][y] = added_piece; //add to board
            this.tile_history.get(y+x*board_size).add(added_piece); // add to tile history
            this.piece_list[13 + i] = added_piece; // add to piece list
        }
    }

    /**
     * the function is setting up the white pieces on the board
     */
    private void SettingUpWhite() {
        // adding the white pieces
        int[][] positions = {{3, 5},
                {4, 4}, {4, 5}, {4, 6},
                {5, 3}, {5, 4}, {5, 5}, {5, 6}, {5, 7},
                {6, 4}, {6, 5}, {6, 6},
                {7, 5}};

        for (int i = 0; i < positions.length; i += 1) {
            int x = positions[i][1];
            int y = positions[i][0];

            if (!(x == 5 && y == 5)){
                ConcretePiece added_piece = new Pawn(this.defending_player, i + 1, new Position(x, y));
                this.tile_history.get(y+x*board_size).add(added_piece);
                this.board_data[x][y] = added_piece;
                this.piece_list[i] = added_piece;
            }
        }

        // adding the king separately
        ConcretePiece added_piece = new King(this.defending_player, 7, new Position(5, 5));
        this.tile_history.get(5+5*board_size).add(added_piece);
        this.board_data[5][5] = added_piece;
        this.piece_list[6] = added_piece;
    }

    /**
     * given a starting position and an ending position it's handling the move
     * @param a The starting position of the piece.
     * @param b The destination position for the piece.
     * @return true if the move is valid otherwise  return false
     */
    @Override
    public boolean move(Position a, Position b) {
        //check that the move is valid
        if (!move_is_valid(a, b)) {
            return false;
        }

        // storing the board before the move is applied
        this.move_history.add(getDeepCopyData(this.board_data));

        // if the move is a valid move then move the piece
        this.move_piece(a, b);
        tile_history.get(b.Y + b.X * board_size).add((ConcretePiece)getPieceAtPosition(b));


        // handling eating situation
        this.eat(b);

        // checking whether the game ended
        isGameFinished();

        //update next player
        this.black_turn = !this.black_turn;
        return true;
    }

    /**
     * given two positiopn return  true if the move from a to b is legal otherwise return false
     * @param a starting position
     * @param b ending position
     * @return
     */
    private boolean move_is_valid(Position a, Position b) {
        int[] move_data = this.move_data(a, b);
        int delta_x = move_data[0];
        int delta_y = move_data[1];
        int dir_x = move_data[2];
        int dir_y = move_data[3];

        //check that the right color piece has been selected
        if (getPieceAtPosition(a).getOwner().isPlayerOne() == this.black_turn) {
            return false;
        }

        // if it's a valid cross move
        if (delta_x * delta_y != 0) {
            return false;
        }

        // check for blocking pieces
        // get y-axis tiles
        for (int i = a.Y + dir_y; i != b.Y; i += dir_y) {
            if (this.getPieceAtPosition(new Position(a.X, i)) != null) {
                return false;
            }
        }
        // get x axis tiles
        for (int i = a.X + dir_x; i != b.X; i += dir_x) {
            if (this.getPieceAtPosition(new Position(i, a.Y)) != null) {
                return false;
            }
        }
        // checking target tile is empty
        if (this.getPieceAtPosition(b) != null) {
            return false;
        }

        //check for a pawn moving to a corner
        if (Objects.equals(getPieceAtPosition(a).getType(), "♟")) {
            return !isCorner(b);
        }
        return true;
    }

    /**
     * given a position b return true or false wether the position is a corner or not
     * @param b
     * @return
     */
    private static boolean isCorner(Position b) {
        return b.same(new Position(10, 0)) ||
                b.same(new Position(10, 10)) ||
                b.same(new Position(0, 10)) ||
                b.same(new Position(0, 0));
    }

    /**
     * an helper function to move
     * @param a
     * @param b
     * @return
     */
    private int[] move_data(Position a, Position b) {
        int delta_x = b.X - a.X;
        int delta_y = b.Y - a.Y;
        int dir_x;
        int dir_y;
        try {
            dir_x = delta_x / Math.abs(delta_x);
        } catch (Exception ArithmeticException) {
            dir_x = 0;
        }
        try {
            dir_y = delta_y / Math.abs(delta_y);
        } catch (Exception ArithmeticException) {
            dir_y = 0;
        }
        return new int[]{delta_x, delta_y, dir_x, dir_y};
    }

    /**
     * swap function in the board for moving a piece
     * @param a - the initial position
     * @param b - the ending position
     */
    private void move_piece(Position a, Position b) {
        Piece moving_piece = this.getPieceAtPosition(a);
        board_data[a.X][a.Y] = null;
        board_data[b.X][b.Y] = (ConcretePiece) moving_piece;

        // keeping track of the piece move
        ((ConcretePiece) moving_piece).addMove(b);
    }

    /**
     * given the previous and the new position of the piece handle the eating
     */
    private void eat(Position b) {

        ConcretePlayer piece_owner = (ConcretePlayer) getPieceAtPosition(b).getOwner();
        ConcretePiece eating_piece = (ConcretePiece) getPieceAtPosition(b);

        if (Objects.equals(eating_piece.getType(), "♚")){
            return;
        }

        //                  up,      right,  down,    left
        int[][] offsets = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};
        for (int[] offset : offsets) {
            Position one_aside = new Position(b.X + offset[0], b.Y - offset[1]);
            Position two_aside = new Position(b.X + offset[0] * 2, b.Y - offset[1] * 2);
            ConcretePiece first_neighbor = (ConcretePiece) getPieceAtPosition(one_aside);
            ConcretePiece second_neighbor = (ConcretePiece) getPieceAtPosition(two_aside);

            // if not null and not king
            if (first_neighbor != null && second_neighbor != null && !first_neighbor.getClass().getName().equals("King")) {
                ConcretePlayer first_neighbor_owner = (ConcretePlayer) first_neighbor.getOwner();
                ConcretePlayer second_neighbor_owner = (ConcretePlayer) second_neighbor.getOwner();

                // if the close tile is an ennemy
                if (piece_owner != first_neighbor_owner) {

                    boolean is_sandwiched = (piece_owner == second_neighbor_owner);
                    boolean is_stuck_between_walls = (second_neighbor_owner.isWall() || isCorner(two_aside));
                    if (is_sandwiched || ((is_stuck_between_walls) && !first_neighbor_owner.isWall())) {
                        eatPieceAtPosition(one_aside);
                        eating_piece.add_Kill();
                    }
                }
            }
        }
    }

    /**
     * given a position get the piece at this position
     * @param position - the wanted position
     * @return - Piece the piece at this position or null if none
     */
    @Override
    public Piece getPieceAtPosition(Position position) {
        try {
            return board_data[position.X][position.Y];
        } catch (Exception IndexOutOfBoundsException) {
            return new Wall();
        }
    }

    /**
     * Given a position get the piece at this position
     * @param X the wanted X of the position
     * @param Y the wanted Y of the position
     * @return - Piece the piece at this position or null if none
     */
    public Piece getPieceAtPosition(int X, int Y) {
        try {
            return board_data[X][Y];
        } catch (Exception IndexOutOfBoundsException) {
            return new Wall();
        }
    }

    /**
     * delete the piece at position X, Y
     */
    private void eatPieceAtPosition(int X, int Y) {
        board_data[X][Y] = null;

    }

    /**
     * delete the piece at position P
     */
    private void eatPieceAtPosition(Position p) {
        eatPieceAtPosition(p.X, p.Y);
    }

    /**
     * Retur the first player
     */
    @Override
    public Player getFirstPlayer() {
        return this.attacking_player;
    }

    /**
     * Retur the second player
     */
    @Override
    public Player getSecondPlayer() {
        return this.defending_player;
    }

    /**
     * checking separately for whit or black win and handling everython if the game is finished
     * @return true if the game is finished or false if not
     */
    @Override
    public boolean isGameFinished() {
        // check for white win
        if (this.white_win()) {
            ConcretePlayer white = (ConcretePlayer) this.getSecondPlayer();
            white.addWin();
            white.setWinner(true);
            this.PrintRecap();
            white.setWinner(false);
            return true;
        }
        // check for black win
        else if (this.black_win()) {
            ConcretePlayer black = (ConcretePlayer) this.getFirstPlayer();
            black.addWin();
            black.setWinner(true);
            this.PrintRecap();
            black.setWinner(false);
            return true;
        }

        return false;
    }

    /**
     * check are the whites won by eating all the black pieces or by successfuly going with the king to a corner
     * @return true if the white pieces won and false if not
     */
    private boolean white_win() {

        // there is only 2 black pieces left
        boolean check_1 = this.onlyTwoBlackPieces();

        // the king has reached a corner
        boolean check_2 = this.KingIsInCorner();

        return check_1 || check_2;
    }

    /**
     * check is there 2 blakck pieces or not to determine the end of a game
     * @return true or false if the condition is fulfilled
     */
    private boolean onlyTwoBlackPieces() {
        int black_count = 0;

        for (int i = 0; i < this.board_size; i += 1) {
            for (int j = 0; j < this.board_size; j += 1) {
                if (board_data[i][j] != null && Objects.equals(board_data[i][j].getOwner().toString(), this.attacking_player.toString())) {
                    black_count += 1;
                    if (black_count > 2) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * return true if the king is in a corner or false if not
     */
    private boolean KingIsInCorner() {
        Piece[] corner_pieces = {
                getPieceAtPosition(0, 10),
                getPieceAtPosition(10, 10),
                getPieceAtPosition(10, 0),
                getPieceAtPosition(0, 0)};

        for (Piece piece : corner_pieces) {
            if (piece != null && Objects.equals(piece.getType(), "♚")) {
                return true;
            }
        }
        return false;
    }

    /**
     * check are the black won by surrounding the white king
     * @return true if the black pieces won and false if not
     */
    private boolean black_win() {

        //finding the position of the king
        Position king_pos = getKingPos();

        //determine if the king is surrounded
        return this.KingIsSurrounded(king_pos);
    }

    /**
     * check whether the white king is surrounded or not
     * @param pos - the position of the king
     * @return true if the king is surrounded and false if not
     */
    private boolean KingIsSurrounded(Position pos) {

        int x = pos.X;
        int y = pos.Y;

        //up
        boolean up = (x - 1 < 0 || (board_data[x - 1][y] != null && board_data[x - 1][y].getOwner() == this.attacking_player));

        //right
        boolean right = (y + 1 >= this.board_data.length || (board_data[x][y + 1] != null && board_data[x][y + 1].getOwner() == this.attacking_player));

        // down
        boolean down = (x + 1 >= this.board_data.length || (board_data[x + 1][y] != null && board_data[x + 1][y].getOwner() == this.attacking_player));

        boolean left = (y - 1 < 0 || (board_data[x][y - 1] != null && board_data[x][y - 1].getOwner() == this.attacking_player));

        return (up && right && down && left);

    }

    private Position getKingPos() {
        Position king_pos = null;
        for (int i = 0; i < this.board_size; i++) {
            for (int j = 0; j < this.board_size; j++) {
                if (this.board_data[i][j] != null && Objects.equals(this.board_data[i][j].getType(), "♚")) {
                    return new Position(i, j);
                }
            }
        }
        return null;
    }

    /**
     * return true or false wether it is the second player turn or not
     */
    @Override
    public boolean isSecondPlayerTurn() {
        return this.black_turn;
    }

    /**
     * reset the game board and all the variables that keep track of a game
     */
    @Override
    public void reset() {
        GameLogic new_game = new GameLogic();

        this.board_data = getDeepCopyData(new_game.board_data);
        this.tile_history = new_game.tile_history;
        this.move_history = new_game.move_history;
        this.piece_list = new_game.piece_list;

        this.black_turn = true;
    }

    /**
     * This function undo a move in the board, currently not working with the second part of the exercise
     */
    @Override
    public void undoLastMove() {
        try {
            ConcretePiece[][] last_state = move_history.pop();
            this.black_turn = !this.black_turn;
            this.board_data = getDeepCopyData(last_state);
        } catch (Exception EmptyStackException) {

        }
    }

    /**
     * retur the board size
     * @return the size of the board
     */
    @Override
    public int getBoardSize() {
        return this.board_size;
    }

    /**
     * A tool for deep copying a 2d array
     *
     * @param original - the original array
     * @return a deep copy of the array
     */
    public static ConcretePiece[][] getDeepCopyData(ConcretePiece[][] original) {
        if (original == null) {
            return null;
        }

        final ConcretePiece[][] result = new ConcretePiece[original.length][];
        for (int i = 0; i < original.length; i++) {
            result[i] = Arrays.copyOf(original[i], original[i].length);
        }
        return result;
    }

    /**
     * a String representation of the board for debugging essentialy
     * @return
     */
    @Override
    public String toString() {
        String string = "";
        for (ConcretePiece[] row : this.board_data) {
            for (ConcretePiece piece : row) {
                if (piece != null) {
                    string += piece.toString();
                } else {
                    string += " .";
                }

            }
            string += "\n";
        }

        return string;
    }

    /**
     * PART 2 of the exercice -
     *
     * Print the history of a game:
     * 1- the history of each pieces move
     * 2- the number of kill each piece has done
     * 3- the total distance of each piece
     * 4- the history of each piece that as stepped on a specific tile
     */
    public void PrintRecap() {
        // print move history
        this.Print_move_history();
        this.print_stars(75);

        // print kill history
        this.Print_kill_history();
        this.print_stars(75);

        // print moving distance history
        this.Print_dist_history();
        this.print_stars(75);

        // print tile history
        this.Print_tile_history();
        this.print_stars(75);

    }

    /**
     * print the kill history accordingly after sorting it accordingly to the exercice
     */
    private void Print_kill_history(){

        Comparator<ConcretePiece> compare_kills = new Compare_kills();
        Arrays.sort(this.piece_list, compare_kills);
        for (ConcretePiece piece : this.piece_list) {
            if (piece.kills > 0){
                System.out.println(piece.kill_hist());
            }
        }
    }

    /**
     * print the move history accordingly after sorting it accordingly to the exercice
     */
    private void Print_move_history() {
        Comparator<ConcretePiece> compare_moves = new Compare_moves();
        Arrays.sort(this.piece_list, compare_moves);
        for (ConcretePiece piece : this.piece_list) {
            if (piece.pos_hist().contains("), (")){
                System.out.println(piece.pos_hist());
            }
        }
    }

    /**
     * print the distance history accordingly after sorting it accordingly to the exercice
     */
    private void Print_dist_history(){

        Comparator<ConcretePiece> compare_dist = new Compare_dist();
        Arrays.sort(this.piece_list, compare_dist);
        for (ConcretePiece piece : this.piece_list) {
            if (piece.calculate_moves_distance() > 0){
                System.out.println(piece.dist_hist());
            }
        }
    }

    /**
     * print the tile history accordingly after sorting it accordingly to the exercice
     */
    private void Print_tile_history(){

        Comparator<MySet<ConcretePiece>> compare_tile = new Compare_tiles();
        this.tile_history.sort(compare_tile.reversed());
        for (int i = 0; i < this.tile_history.size(); i+=1) {
            MySet<ConcretePiece> single_tile_hist = this.tile_history.get(i);
            if (single_tile_hist.size() > 1){
                System.out.println("(" + (single_tile_hist.pos.X) + ", " + (single_tile_hist.pos.Y) +")"+ single_tile_hist.size() +" pieces");
            }
        }
    }

    /**
     * print stars to seperate each part of the exercice
     * @param num - the number of starts to print
     */
    private void print_stars(int num) {
        String string = "";
        for (int i = 0; i < num; i++) {
            string += "*";
        }

        System.out.println(string);
    }
}
