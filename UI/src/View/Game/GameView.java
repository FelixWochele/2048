package View.Game;

import AI.IAIPlayer;
import Factory.ComponentFactory;
import DataClasses.Directions;
import DataClasses.GameModes;
import DataClasses.GameOptions;
import DataClasses.Tile;
import Game.IGameController;
import HighScore.IHighscoreController;
import View.Highscore.IHighscoreView;
import View.Menue.MenueView;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GameView implements IGameView {

    //Binding Variablen
    @FXML
    private Label myLabel;
    @FXML
    private Label highscoreLabel;
    @FXML
    private Label scoreLabel;

    //Globale Variablen

    //Fenstergrößen
    private int minWindowWidth;
    private int minWindowHeight;
    private int windowWidth;
    private int windowHeight;

    //Größen der Spielfeldelemente
    private double gameBoardSize;
    private double gameBoardGap;
    private double tileSize;
    private double scoreBoxSize;

    private boolean aiMode;
    private int aiRepetitions;
    private int tileCount;
    private GameModes gameMode;
    private boolean inMoveFlag = false;
    private boolean inMoveFlagShortMove = false;
    private int gameStatus = 0;
    private final int transitionTimeOfTiles = 48; //in millisekunden

    //Spielfelder die übergeben werden
    private Tile[][] nextGameBoard;
    private Tile[][] prevGameBoard;

    private Scene scene;
    //Ist das "Spielfeld" worauf alles hinzugefügt und bewegt wird.
    private Pane pane;

    private final IGameController gameController;
    private final IHighscoreController highScoreController;
    private IAIPlayer aiPlayer;

    private GameOptions gameOptions;
    private IHighscoreView highscoreView;

    private Thread aiThread;



    /**
     * Kosntruktor
     */
    public GameView() {
        this.gameController = ComponentFactory.getGameController();
        this.highScoreController = ComponentFactory.getHighScoreController();
    }

    /**
     * Erzeugt einen neue Scene für den Controller, wird vom Modusmenue aufgerufen
     * @param event
     * @throws IOException
     */
    @Override
    public void createGameScene(Event event, Scene scene) throws IOException {

        //Variablen mit einstellbaren Konstanten
        gameBoardSize = 450; // Bei verstellen müssen noch die Stadart fxml werte geändert werden -> Schriftgröße/png größen/etc
        gameBoardGap = gameBoardSize * 0.02;
        tileSize = (gameBoardSize - (gameBoardGap * (tileCount + 1))) / tileCount;
        scoreBoxSize = ((gameBoardSize / 3) * 0.7);

        this.gameController.setTileSize(tileSize, tileCount);

        //Hole das Pane(/board) aus der .fxml anhand der ID
        pane = (Pane) scene.lookup("#board");
        this.scene = scene;

        //Setze die größe des Panes auf die vorgegebenen
        pane.setPrefSize(gameBoardSize, gameBoardSize);
        pane.setMinHeight(gameBoardSize);
        pane.setMaxHeight(gameBoardSize);
        pane.setMinWidth(gameBoardSize);
        pane.setMaxWidth(gameBoardSize);

        //Legt die größen der Elemente in Abhängigkeit der größe der Spielfelds fest
        HBox hBox = (HBox) scene.lookup("#gameBoardHBox");
        hBox.setPrefWidth(gameBoardSize);
        hBox.setMinWidth(gameBoardSize);

        //Unterteilt die Anzeigeelemente (obehalb gamboard) in 3 Abschnitte (jeweils 1/3 vom gamboard breit)
        VBox vBoxL = (VBox) scene.lookup("#gameBoardVBoxL");
        vBoxL.setPrefWidth(gameBoardSize / 3);
        vBoxL.setMinWidth(gameBoardSize / 3);

        VBox vBoxM = (VBox) scene.lookup("#gameBoardVBoxM");
        vBoxL.setPrefWidth(gameBoardSize / 3);
        vBoxL.setMinWidth(gameBoardSize / 3);

        VBox vBoxR = (VBox) scene.lookup("#gameBoardVBoxR");
        vBoxR.setPrefWidth(gameBoardSize / 3);
        vBoxR.setMinWidth(gameBoardSize / 3);

        Pane highscoreBox = (Pane) scene.lookup("#highscoreBox");
        highscoreBox.setPrefWidth(scoreBoxSize);
        highscoreBox.setPrefHeight(scoreBoxSize);
        highscoreBox.setMaxWidth(scoreBoxSize);
        highscoreBox.setMaxHeight(scoreBoxSize);

        Pane scoreBox = (Pane) scene.lookup("#scoreBox");
        scoreBox.setPrefWidth(scoreBoxSize);
        scoreBox.setPrefHeight(scoreBoxSize);
        scoreBox.setMaxWidth(scoreBoxSize);
        scoreBox.setMaxHeight(scoreBoxSize);

        Button buttonMenue = (Button) scene.lookup("#buttonMenue");
        buttonMenue.setPrefWidth(scoreBoxSize);
        buttonMenue.setPrefHeight(scoreBoxSize / 4);
        buttonMenue.setMaxWidth(scoreBoxSize);
        buttonMenue.setMaxHeight(scoreBoxSize / 4);

        Button buttonHighscore = (Button) scene.lookup("#buttonHighscore");
        buttonHighscore.setPrefWidth(scoreBoxSize);
        buttonHighscore.setPrefHeight(scoreBoxSize / 4);
        buttonHighscore.setMaxWidth(scoreBoxSize);
        buttonHighscore.setMaxHeight(scoreBoxSize / 4);

        //Erzeuge die Hintergurnd Tiles wie bei einem 2D-Array
        for (int j = 0; j < tileCount; j++) {
            for (int k = 0; k < tileCount; k++) {

                //Errechne die Position der einzelnen Tiles
                double posX = gameBoardGap * (j + 1) + (tileSize * j);
                double posY = gameBoardGap * (k + 1) + (tileSize * k);

                //Erzeuge Rechteck für die Tiles
                Rectangle rectangle = new Rectangle(posX, posY, tileSize, tileSize);
                rectangle.setArcWidth(20);
                rectangle.setArcHeight(20);
                rectangle.setFill(Color.rgb(204, 192, 179));

                pane.getChildren().add(rectangle);
            }
        }

        //Erzeuge eine neue Stage für die GameView
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(scene);
        stage.show();

        //Setzt die eingestellten Gamoptions im Controler
        gameOptions = new GameOptions( aiMode, tileCount, gameMode, aiRepetitions);
        this.aiPlayer = ComponentFactory.getAIPlayer(this.gameOptions);
        //Initalisiert im gameController ein Spielfeld

        highScoreController.setGameOptions(gameOptions);

        //Setzt ein leeres altes Spielfeld und führt den ersten Move aus um das Spielfeld im UI zu erzeugen
        prevGameBoard = new Tile[tileCount][tileCount];

        //Keylistener auf der Scene
        if(!aiMode)
        {
            scene.setOnKeyReleased(e -> {

                //Wenn ein Zug im Gange ist kann kein neuer ausgeführt werden -> verhindert asynchronität vom Contoler und dem UI
                if(!inMoveFlag) {
                    switch (e.getCode()) {
                        case W -> {
                            //Falg das gerade ein Zug ausgeführt wird
                            inMoveFlag = true;
                            //Lässt den Controler ein neues Spielfeld erzeugen
                            gameController.makeMove(Directions.UP);
                            //Führt den Zug im UI aus
                     //       move();
                        }
                        case S -> {
                            //Falg das gerade ein Zug ausgeführt wird
                            inMoveFlag = true;
                            //Lässt den Controler ein neues Spielfeld erzeugen
                            gameController.makeMove(Directions.DOWN);
                            //Führt den Zug im UI aus
                         //   move();
                        }
                        case A -> {
                            //Falg das gerade ein Zug ausgeführt wird
                            inMoveFlag = true;
                            //Lässt den Controler ein neues Spielfeld erzeugen
                            gameController.makeMove(Directions.LEFT);
                            //Führt den Zug im UI aus
                          //  move();
                        }
                        case D -> {
                            //Falg das gerade ein Zug ausgeführt wird
                            inMoveFlag = true;
                            //Lässt den Controler ein neues Spielfeld erzeugen
                            gameController.makeMove(Directions.RIGHT);
                            //Führt den Zug im UI aus
                          //  move();
                        }
                        case PLUS -> {
                            //Score Test (EASTER EGG)
                            int var = Integer.parseInt(scoreLabel.getText());
                            if (var == 0) {
                                var = 1;
                            }
                            setScoreLabel(2 * var);
                        }
                        case UP -> {
                            //Falg das gerade ein Zug ausgeführt wird
                            inMoveFlag = true;
                            //Lässt den Controler ein neues Spielfeld erzeugen
                            gameController.makeMove(Directions.UP);
                            //Führt den Zug im UI aus
                            //       move();
                        }
                        case DOWN -> {
                            //Falg das gerade ein Zug ausgeführt wird
                            inMoveFlag = true;
                            //Lässt den Controler ein neues Spielfeld erzeugen
                            gameController.makeMove(Directions.DOWN);
                            //Führt den Zug im UI aus
                            //   move();
                        }
                        case LEFT-> {
                            //Falg das gerade ein Zug ausgeführt wird
                            inMoveFlag = true;
                            //Lässt den Controler ein neues Spielfeld erzeugen
                            gameController.makeMove(Directions.LEFT);
                            //Führt den Zug im UI aus
                            //  move();
                        }
                        case RIGHT -> {
                            //Falg das gerade ein Zug ausgeführt wird
                            inMoveFlag = true;
                            //Lässt den Controler ein neues Spielfeld erzeugen
                            gameController.makeMove(Directions.RIGHT);
                            //Führt den Zug im UI aus
                            //  move();
                        }
                    }
                }
            });
        }

        this.gameController.setTileChangeListener(tiles ->
        {

            nextGameBoard = tiles;
            move();

            try
            {

                    if (aiMode)
                    {
                        aiThread =  new Thread(() ->
                        {
                            try
                            {
                                Thread.sleep(100);
                                if(!(gameStatus==3||gameStatus==4))
                                {
                                    makeAIMove();
                                }

                            } catch (InterruptedException e)
                            {
                                e.printStackTrace();
                            }
                        });
                        aiThread.start();
                    }
            }
            catch (Exception e)
            {

            }
        });

        this.gameController.setScoreChangeListener(newScore -> {
            setScoreLabel(newScore);
            highScoreController.submitNewScore(newScore);
            setHighscore(highScoreController.getCurrentHighScoreData().getScore());
        });
        gameController.startGame(gameOptions);
        setHighscore(highScoreController.getCurrentHighScoreData().getScore());
    }

    /**
     * Ruft die Ai auf, welche den besten Zug berechnet
     * Führt den zurückgegebenen Zug aus
     */
    private void makeAIMove()
    {
        var aiDirection = aiPlayer.calculateNextDirection(prevGameBoard);
        Platform.runLater(() -> {
            gameController.makeMove(aiDirection);});
    }

    /**
     * Wird beim Drücken des Buttons "Menu" ausgeführt -> Öffnet die Menü Scene
     * @param event
     * @throws IOException
     */
    public void onButtonPressMenue(Event event) throws IOException {

        if(aiThread!=null)
        aiThread.stop();
        this.gameStatus = 3;
        gameController.calculateElapsedTime(System.currentTimeMillis());
        //Erzeuge eine Szene aus ModusMenueView.fxml
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/View/Menue/MenueView.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root, windowWidth, windowHeight);

        //Wird um den Controller zu laden benötigt ... obwohl die variable nicht benutzt wird
        MenueView menueView = loader.getController();

        //Erzeuge eine neue Stage für die GameView
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Generiert aus dem gegegbenen Array das Spielfeld im UI
     * Das Array wird vom gameControler erzeugt, welcher vor dieser Funktion aufgerufen wird
     *
     */
    private void move() {

        //Flag ob ein neues Tile erzeugt wurde
        boolean newTileFlag = true;

        //Für die Freigabe des nächsten zuges wenn keine lange transition stattfand (-> mergeCount = 0)
        int mergeCount = 0;

        //Falls aus einem grund kein Update mehr kommt darf auch nichtsmehr passieren -> sonst gibts einen Exception
        int testForDuplicateCounter = 0;
        //setzt die einzelnen Werte von nextGameBoard in prevGameBoard
        for (int j = 0; j < tileCount; j++) {
            for (int k = 0; k < tileCount; k++) {

                if (prevGameBoard[j][k] == nextGameBoard[j][k]) {
                    testForDuplicateCounter++;
                }
            }
        }
        if (testForDuplicateCounter <= (tileCount * 2) - 1) {
            inMoveFlagShortMove = true;
        }

        //Itteriert über das Spielfeld
        for (int posX = 0; posX < tileCount; posX++) {
            for (int posY = 0; posY < tileCount; posY++) {

                //Fall wenn keine preTiles da sind -> Generiert ein neues Element
                if (nextGameBoard[posX][posY] != null && nextGameBoard[posX][posY].checkForPreTiles()) {

                    //Sucht im letzten Spielfeld nach dem Tile
                    for (int r = 0; r < tileCount; r++) {
                        for (int s = 0; s < tileCount; s++) {
                            if (nextGameBoard[posX][posY] == prevGameBoard[r][s]) {
                                //make transition
                                transition(nextGameBoard[posX][posY], posX, posY);

                                newTileFlag = false;
                                break;
                            }
                        }
                    }

                    //Wenn im letzten Spielfeld kein Tile gefunden wurde wird hier ein neues erzeugt
                    if (newTileFlag) {

                        //Add new Tile
                        Pane tilePane = nextGameBoard[posX][posY].getPane();

                        tilePane.setLayoutX(gameBoardGap * (posX + 1) + (tileSize * posX));
                        tilePane.setLayoutY(gameBoardGap * (posY + 1) + (tileSize * posY));

                        pane.getChildren().add(tilePane);

                        zoomInTile(nextGameBoard[posX][posY]);

                    } else {
                        newTileFlag = true;
                    }
                }
                //Fall preTiles vorhanden sind -> animation der preTiles und darstellen des neuen Tiles
                else if (nextGameBoard[posX][posY] != null) {

                    transitionChildren(nextGameBoard[posX][posY], posX, posY);
                    mergeCount++;
                }
            }
        }

        //setzt die einzelnen Werte von nextGameBoard in prevGameBoard (einfaches kopieren)
        for (int j = 0; j < tileCount; j++) {
            for (int k = 0; k < tileCount; k++) {
                prevGameBoard[j][k] = nextGameBoard[j][k];
            }
        }

        if(mergeCount == 0){
            //Wenn keine lange transition stattfand und man nicht warten muss kann der nächste Zug kann nach einer kurzen transition freigegeben werden
            inMoveFlagShortMove = true;
        }

        //Gewinnabfrage
        gameStatus = this.gameController.getGameStatus();
        winLoseScreen();
    }

    /**
     * Führt die Animation des Tiles aus, wenn es keine Kinder hat
     *
     * @param tile                      Tile welches die Animation ausführt
     * @param posX                      Zielposition in X
     * @param posY                      Zielposition in Y
     */
    private void transition(Tile tile, int posX, int posY) {

        TranslateTransition translateTransition = new TranslateTransition();

        Pane tilePane = tile.getPane();

        translateTransition.setToX((gameBoardGap * (posX + 1) + (tileSize * posX)) - tilePane.getLayoutX());
        translateTransition.setToY((gameBoardGap * (posY + 1) + (tileSize * posY)) - tilePane.getLayoutY());
        translateTransition.setNode(tilePane);
        translateTransition.setDuration(new Duration(transitionTimeOfTiles));
        translateTransition.play();

        translateTransition.setOnFinished(g -> afterShortMove());
    }

    /**
     * Wenn es keine Zusammenführung mehrerer Tiles gibt kann der
     * Zug nach Ende einer kurzen Transition freigegeben werden, was mit dem Aufruf dieser Mehtode passiert
     */
    private void afterShortMove(){
        if(inMoveFlagShortMove){
            inMoveFlag = false;
        }
    }

    /**
     * Führt die Animation aus, wenn kinder vorhanden sind
     *
     * @param tile Parent Tile
     * @param posX Zielposition in X
     * @param posY Zielposition in Y
     */
    private void transitionChildren(Tile tile, int posX, int posY) {

        TranslateTransition translateTransitionA = new TranslateTransition();
        TranslateTransition translateTransitionB = new TranslateTransition();

        Pane tilePaneA = tile.getPreFieldA().getPane();
        Pane tilePaneB = tile.getPreFieldB().getPane();

        //Definiert die Transition
        translateTransitionA.setToX((gameBoardGap * (posX + 1) + (tileSize * posX)) - tilePaneA.getLayoutX());
        translateTransitionA.setToY((gameBoardGap * (posY + 1) + (tileSize * posY)) - tilePaneA.getLayoutY());
        translateTransitionA.setNode(tilePaneA);
        translateTransitionA.setDuration(new Duration(transitionTimeOfTiles));

        translateTransitionB.setToX((gameBoardGap * (posX + 1) + (tileSize * posX)) - tilePaneB.getLayoutX());
        translateTransitionB.setToY((gameBoardGap * (posY + 1) + (tileSize * posY)) - tilePaneB.getLayoutY());
        translateTransitionB.setNode(tilePaneB);
        translateTransitionB.setDuration(new Duration(transitionTimeOfTiles));

        //Führt die Animation aus
        translateTransitionA.play();
        translateTransitionB.play();

        //Löscht bzw generiert Tiles nachdem die Animation ausgeführt ist
        translateTransitionA.setOnFinished(f -> createTile(tile, posX, posY));
        translateTransitionB.setOnFinished(f -> removeTile1(tile));
    }

    /**
     * Generiet ein neues Tile nach der Animation der preTiles
     *
     * @param tile Tile welches dargestellt wird
     * @param posX Position in X an der es dargestellt wird
     * @param posY Position in Y an der es dagestellt wird
     */
    private void createTile(Tile tile, int posX, int posY) {

        tile.getPane().setLayoutX(gameBoardGap * (posX + 1) + (tileSize * posX));
        tile.getPane().setLayoutY(gameBoardGap * (posY + 1) + (tileSize * posY));

        pane.getChildren().add(tile.getPane());

        zoomInTile(tile);
    }

    /**
     * Entfernt das Tile bzw die Pane vom grafischen Spielfeld (Erstes Kind)
     * Ruft danach das Löschen des Zweiten Kindes auf
     *
     * @param tile Zu entfernendes Tile
     */
    private void removeTile1(Tile tile) {
        //Try/Catch zur sicherheit, falls Controler/Logik fehler
        try {
            pane.getChildren().remove(tile.getPreFieldA().getPane());
        }catch(Exception e){
            System.out.println("Cant remove Tile");
        }
        removeTile2(tile);
    }
    /**
     * Entfernt das Tile bzw die Pane vom grafischen Spielfeld (Zweites Kind)
     *
     * @param tile Zu entfernendes Tile
     */
    private void removeTile2(Tile tile) {
        //Try/Catch zur sicherheit, falls Controler/Logik fehler
        try {
            pane.getChildren().remove(tile.getPreFieldB().getPane());
        }catch(Exception e){
            System.out.println("Cant remove Tile");
        }
        inMoveFlag = false;
    }

    /**
     * Tiles für einen Zoomefeckt um 10% vergrößern
     *
     * @param tile Tile auf welches der Effeckt angewendet wird
     */
    private void zoomInTile(Tile tile) {

        ScaleTransition scaleTransition = new ScaleTransition();
        scaleTransition.setDuration(new Duration(50));
        scaleTransition.setNode(tile.getPane());
        scaleTransition.setByX(0.1f);
        scaleTransition.setByY(0.1f);
        scaleTransition.play();

        scaleTransition.setOnFinished(a -> zoomOutTile(tile));
    }

    /**
     * Tiles für einen Zoomefeckt auf Standart größe verkleinern
     *
     * @param tile Tile auf welches der Effeckt angewendet wird
     */
    private void zoomOutTile(Tile tile) {

        ScaleTransition scaleTransition = new ScaleTransition();
        scaleTransition.setDuration(new Duration(50));
        scaleTransition.setNode(tile.getPane());
        scaleTransition.setByX(-0.1f);
        scaleTransition.setByY(-0.1f);
        scaleTransition.play();
    }

    /**
     * Gewinnabfrage -> Wenn gewonnen zeige Siegerbildschirm, wenn verloren Verliererbildschirm
     */
    private void winLoseScreen() {


        //gameStatus Im Spiel -> 0 | Wenn Gewonnen -> 1 | Nach gewinn weiterspielen -> 2 | Verloren -> 3

        //Wenn das erste Mal gewonnen wurde wird der Gewinnerscreen angezeigt
        if (gameStatus == 1) {

            StackPane winnerPane = new StackPane();
            winnerPane.setPrefHeight(gameBoardSize);
            winnerPane.setPrefWidth(gameBoardSize);
            winnerPane.setId("winPane");
            winnerPane.toFront();
            pane.getChildren().add(winnerPane);

            Text winText = new Text("Gewonnen!");
            winText.setId("winText");
            winnerPane.getChildren().add(winText);

            gameStatus++;
        }
        //Wenn gewonnen wurde aber man noch weiterspielt wird der Gewinnerscreen wieder entfernt
        else if (gameStatus == 2) {
            pane.getChildren().remove(scene.lookup("#winPane"));
            pane.getChildren().remove(scene.lookup("#winText"));
        }
        else if (gameStatus == 3) {
            if(aiThread!=null) {
                aiThread.stop();
            }
            gameController.calculateElapsedTime(System.currentTimeMillis());
            StackPane loserPane = new StackPane();
            loserPane.setPrefHeight(gameBoardSize);
            loserPane.setPrefWidth(gameBoardSize);
            loserPane.setId("winPane");
            loserPane.toFront();
            pane.getChildren().add(loserPane);

            Text winText = new Text("Verloren!");
            winText.setId("winText");
            loserPane.getChildren().add(winText);

            //Das man keinen weiteren move machen kann wird einfach inMovoe gesetzt
            inMoveFlag = true;
        }
    }

    /**
     * Setzt den KiMode, kommt vom Main menü
     *
     * @param aiMode Der zu setzende Modus
     */
    @Override
    public void setAiMode(boolean aiMode, int repetitions) {
        this.aiMode = aiMode;
        this.aiRepetitions = repetitions;
    }

    /**
     * Setze die anzahl der Tiles, kommt vom Main menü
     *
     * @param tileCount Anzahl der Tiles
     */
    @Override
    public void setTileCount(int tileCount) {
        this.tileCount = tileCount;
    }

    /**
     * Setzt den Higscore, wird beim Fensteraufruf gesetzt
     *
     * @param highscore Highscore
     */
    @Override
    public void setHighscore(int highscore) {
        highscoreLabel.setText(Integer.toString(highscore));
    }


    /**
     * Setzt den Score, wird nach jedem Zug aufgerufen
     *
     * @param score Score
     */
    @Override
    public void setScoreLabel(int score) {
        scoreLabel.setText(Integer.toString(score));
    }

    @Override
    public void setGameMode(GameModes gameMode) {
        this.gameMode = gameMode;
    }

    /**
     * Bekommt die Fenster größe von der Vorgänger Methode
     *
     * @param windowWidth     Fensterhöhe
     * @param windowHeight    Fensterbreite
     * @param minWindowWidth  Maximale Fensterbreite
     * @param minWindowHeight Maximale Fensterhöhe
     */
    public void setWindowDimensions(int windowWidth, int windowHeight, int minWindowWidth, int minWindowHeight) {
        this.windowWidth = windowWidth;
        this.windowHeight = windowHeight;
        this.minWindowWidth = minWindowWidth;
        this.minWindowHeight = windowHeight;
    }

    /**
     * Wenn der Highscore Button gedrückt wird, wird diese Methode ausgeführt
     *
     * @param event Event
     * @throws IOException Exception
     */
    public void onButtonPressHighscore(ActionEvent event) throws IOException
    {
        if(aiThread!=null) {
            aiThread.stop();
        }
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/View/Highscore/HighscoreView.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root, windowWidth, windowHeight);

        highscoreView = loader.getController();
        highscoreView.createSceneHighscore(event, scene);
        highscoreView.setWindowDimensions(windowWidth, windowHeight, minWindowWidth, minWindowHeight);
    }
}
