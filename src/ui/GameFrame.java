package ui;

import graph.MazeGraph.Direction;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.HierarchyEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.beans.PropertyChangeListener;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import model.GameModel;
import model.GameModel.GameState;
import util.Randomness;

/**
 * A window that allows playing an interactive game of PacMann.  Consists of a score/lives label,
 * buttons for pausing the current game and creating a new game, and a game board view that supports
 * keyboard input.
 */
public class GameFrame extends JFrame implements KeyListener {

    /* ****************************************************************
     * Fields for the application's model                             *
     ******************************************************************/

    /**
     * The possible states of the graphical application.
     * <p> RUNNING = The game is actively running
     * <p> PAUSED = A life has started, but the game is not actively running
     * <p> LIFESTART = Ready to begin a new life
     */
    public enum PlayState {RUNNING, PAUSED, LIFESTART}

    /**
     * The current state of this application. To ensure the change propagates correctly, the value
     * of this field should only be updated through the `updatePlayState.accept()` method.
     */
    private PlayState state;

    /**
     * Creates new random game models with the parameters passed to the `GameFrame` constructor.
     */
    private final Supplier<GameModel> modelBuilder;

    /**
     * The state and logic of the current game being played in this window.
     */
    private GameModel model;

    /* ****************************************************************
     * Fields for the application's view                              *
     ******************************************************************/

    /**
     * The component for displaying the current game state and responding to user input related to
     * game actions.
     */
    private GameBoard gameBoard;

    /* ****************************************************************
     * Field for the application's controller                         *
     ******************************************************************/

    /**
     * The amount of time (in milliseconds) that elapses between game frames.
     */
    public static final int FRAME_DURATION = 16; // milliseconds

    /**
     * Fires events at a regular interval to signal the drawing of the next frame
     */
    private final Timer timer;

    /**
     * Used to update the score label in response to a change in the score/life count in the model.
     */
    private PropertyChangeListener updateScoreLabel;

    /**
     * Used to update the text of the play/pause button in response to a change in the state.
     */
    private Consumer<PlayState> updatePlayState;

    /* ****************************************************************
     * Constructor                                                    *
     ******************************************************************/

    /**
     * Create a new window for playing interactive games of PacMann.  All games will have boards
     * with `height` rows and `width` columns.  If `withAI` is true, the player actor will be
     * controlled by AI; otherwise, it will be controlled by user input.  If `showPaths` is true,
     * then the "guidance paths" of actors will be displayed (for debugging purposes).  `seed`
     * determines the sequence of random values used in map creation and AI logic.
     */
    public GameFrame(int width, int height, boolean withAI, long seed) {
        super("PacMann");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        timer = new Timer(FRAME_DURATION, e -> model.updateActors(FRAME_DURATION));
        initializeComponentHierarchy();
        gameBoard.requestFocusInWindow(); // Give the game board keyboard focus

        // set up model
        final Randomness[] randomness = {new Randomness(seed)};
        // Here, we use this "array trick" to capture the `Randomness` object within the lambda.
        // We don't need the `Randomness` to be a field accessible elsewhere within this class.
        modelBuilder = () -> {
            randomness[0] = randomness[0].next();
            return GameModel.newGame(width, height, withAI, randomness[0]);
        };
        newGame();

        // Auto-pause when window is hidden/closed
        addHierarchyListener((HierarchyEvent e) -> {
            if (((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0)
                    && !isShowing() && state == PlayState.RUNNING) {
                timer.stop();
                updatePlayState.accept(PlayState.PAUSED);
            }
        });
        pack();
    }

    /* ****************************************************************
     * View / Controller set-up                                       *
     ******************************************************************/

    /**
     * Initializes and arranges the components in the application frame. The top of the frame
     * includes a label that displays the users total score and remaining number of lives. The
     * middle of the frame shows the Pac-Mann `GameBoard` panel. The bottom of the frame includes
     * two buttons, one to start a new game and the other to start/pause the current game.
     */
    private void initializeComponentHierarchy() {
        add(createScoreLabel(), BorderLayout.NORTH);

        gameBoard = new GameBoard();
        gameBoard.addKeyListener(this);
        add(gameBoard, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(true);
        buttonPanel.setBackground(Color.BLACK);

        buttonPanel.add(createGraphToggle());
        buttonPanel.add(createNewGameButton());
        buttonPanel.add(createPlayPauseButton());
        buttonPanel.add(createPathToggle());

        add(buttonPanel, BorderLayout.SOUTH);
    }

    /**
     * Constructs and returns a JCheckBox that allows the user to toggle whether the vertices
     * and edges of the maze graph are visualized in the application.
     */
    private JCheckBox createGraphToggle() {
        JCheckBox showGraphToggle = new JCheckBox("Show Graph");
        showGraphToggle.setForeground(Color.WHITE);
        showGraphToggle.setRequestFocusEnabled(false);
        showGraphToggle.addActionListener( e -> gameBoard.toggleGraph());
        return showGraphToggle;
    }

    /**
     * Constructs and returns a JLabel that displays the current score and number of remaining lives
     * to the user. The text of this label can be updated using the `updateScoreLabel` listener.
     */
    private JLabel createScoreLabel() {
        JLabel scoreLabel = new JLabel("Score  |  Lives", SwingConstants.CENTER);
        scoreLabel.setOpaque(true);
        scoreLabel.setBorder(new EmptyBorder(10, 10, 10, 10));
        scoreLabel.setBackground(Color.BLACK);
        scoreLabel.setForeground(Color.WHITE);
        scoreLabel.setFont(scoreLabel.getFont().deriveFont(30.0f));
        updateScoreLabel = e -> scoreLabel.setText("Score: " + model.score() + "  |  Lives: " + model.numLives());
        return scoreLabel;
    }

    /**
     * Constructs and returns a "new game" button that stops the current game (if it is running) and
     * resets the `GameModel` to refer to a newly-constructed maze.
     */
    private JButton createNewGameButton() {
        JButton newGameButton = new JButton("New game");
        newGameButton.setFont(newGameButton.getFont().deriveFont(20.0f));
        newGameButton.setRequestFocusEnabled(false);
        newGameButton.addActionListener(e -> {
            timer.stop();
            newGame();
        });
        return newGameButton;
    }

    /**
     * Constructs and returns a "play/pause" button that allows the user to toggle between the running
     * and not running states of the game. The text of this button is updated to match the current
     * application state using the `updatePlayState` listener.
     */
    private JButton createPlayPauseButton() {
        JButton playPauseButton = new JButton("Start");
        playPauseButton.setFont(playPauseButton.getFont().deriveFont(20.0f));
        playPauseButton.setRequestFocusEnabled(false); // Allows board to process all key input
        playPauseButton.addActionListener(e -> processStartPause());
        updatePlayState = (newState) -> {
            state = newState;
            if (state == PlayState.RUNNING) {
                timer.start();
            } else {
                timer.stop();
            }
            playPauseButton.setText(switch (state) {
                case PlayState.LIFESTART -> "Start";
                case PlayState.RUNNING -> "Pause";
                case PlayState.PAUSED -> "Play";
            });
        };
        return playPauseButton;
    }

    /**
     * Processes a press of the start/pause button. Toggles between the RUNNING and PAUSED
     * PlayStates.
     */
    public void processStartPause() {
        updatePlayState.accept(state == PlayState.RUNNING ? PlayState.PAUSED : PlayState.RUNNING);
    }

    /**
     * Constructs and returns a JCheckBox that allows the user to toggle whether the vertices
     * and edges of the maze graph are visualized in the application.
     */
    private JCheckBox createPathToggle() {
        JCheckBox showPathToggle = new JCheckBox("Show Chase Paths");
        showPathToggle.setForeground(Color.WHITE);
        showPathToggle.setRequestFocusEnabled(false);
        showPathToggle.addActionListener( e -> gameBoard.togglePaths());
        return showPathToggle;
    }

    /* ****************************************************************
     * New Game Logic (Model)                                         *
     ******************************************************************/

    /**
     * Replace the current game with a new game corresponding to the next source of randomness and
     * update the UI widgets and listeners to reflect this change.
     */
    private void newGame() {
        updatePlayState.accept(PlayState.LIFESTART);

        if (model != null) {
            model.removeAllPropertyChangeListeners(); // clean up stale listeners in old model
        }

        model = modelBuilder.get(); // generate new random model
        gameBoard.setModel(model); // update board to use new game model

        model.addPropertyChangeListener("score", updateScoreLabel);
        model.addPropertyChangeListener("lives", updateScoreLabel);
        updateScoreLabel.propertyChange(null); // initialize score label

        model.addPropertyChangeListener("game_state", e -> {
            GameState newState = (GameState) e.getNewValue();
            if (newState != GameState.PLAYING) {
                timer.stop();
            }
            if (newState == GameState.VICTORY) {
                showWinMessage();
            } else if (newState == GameState.DEFEAT) {
                showLoseMessage();
            } else {
                updatePlayState.accept(PlayState.LIFESTART);
            }
        });
    }

    /**
     * Show a modal dialog indicating that the current game has been won.
     */
    private void showWinMessage() {
        JOptionPane.showMessageDialog(this, "You win! Final score: " +
                model.score(), "Victory!", JOptionPane.INFORMATION_MESSAGE);
        newGame();
    }

    /**
     * Show a modal dialog indicating that the current game has been lost.
     */
    private void showLoseMessage() {
        JOptionPane.showMessageDialog(this, "You lose. Final score: " +
                model.score(), "Defeat", JOptionPane.INFORMATION_MESSAGE);
        newGame();
    }

    /* ****************************************************************
     * KeyListener Interface                                          *
     ******************************************************************/

    @Override
    public void keyTyped(KeyEvent e) {
        // do nothing
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int keyCode = e.getKeyCode();
        boolean directionalKeyPressed = false;

        if (keyCode == KeyEvent.VK_UP || keyCode == KeyEvent.VK_W) {
            model.updatePlayerCommand(Direction.UP);
            directionalKeyPressed = true;
        } else if (keyCode == KeyEvent.VK_DOWN || keyCode == KeyEvent.VK_S) {
            model.updatePlayerCommand(Direction.DOWN);
            directionalKeyPressed = true;
        } else if (keyCode == KeyEvent.VK_RIGHT || keyCode == KeyEvent.VK_D) {
            model.updatePlayerCommand(Direction.RIGHT);
            directionalKeyPressed = true;
        } else if (keyCode == KeyEvent.VK_LEFT || keyCode == KeyEvent.VK_A) {
            model.updatePlayerCommand(Direction.LEFT);
            directionalKeyPressed = true;
        } else if (keyCode == KeyEvent.VK_SPACE) {
            this.processStartPause();
        }

        if (directionalKeyPressed && (state == PlayState.PAUSED || state == PlayState.LIFESTART)) {
            updatePlayState.accept(PlayState.RUNNING);
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        // do nothing
    }
}
