package sample;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.geometry.Orientation;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PlaySceneController {
    public Rectangle boatSubmarine;
    public Rectangle boatCarrier;
    public Rectangle boatCruiser;
    public Rectangle boatDestroyer;
    public Rectangle boatBattleship;
    public GridPane playerBoard;
    public TextArea textArea;
    public Button startButton;
    public GridPane PCBoard;
    public Button randomButton;
    public ProgressBar PCHealth;
    public ProgressBar playerHealth;

    private Image boatFiveImage = new Image("sample/boatFive.png");
    private Image boatFourImage = new Image("sample/boatFour.png");
    private Image boatThree1Image = new Image("sample/boatThree1.png");
    private Image boatThree2Image = new Image("sample/boatThree2.png");
    private Image boatTwoImage = new Image("sample/boatTwo.png");

    public Pane scene2;
    //polja jedne i druge ploce
    private List<Rectangle> playerBoardFields = new ArrayList();
    private List<Rectangle> PCBoardFields = new ArrayList();

    private Bounds playerBoardBounds;
    private Board player = new Board();
    private Board PC = new Board();
    private List<Rectangle> ships = new ArrayList();
    private boolean firstTime = true;

    private RandomAI randomAI = new RandomAI();
    private SequenceAI sequenceAI = new SequenceAI();
    private StrategyOneAI strategyOneAI = new StrategyOneAI();
    private StrategyTwoAI strategyTwoAI = new StrategyTwoAI();

    @FXML
    public void initialize() {

        //radi prevlacenja brodica na player plocu
        for (Node currentNode : playerBoard.getChildren()) {
            if (currentNode instanceof Rectangle) {
                playerBoardFields.add((Rectangle) currentNode);
            }
        }
        player.setFields(playerBoardFields);

        //dodavanje u listu PC polja
        for (Node currentNode : PCBoard.getChildren()) {
            if (currentNode instanceof Rectangle) {
                PCBoardFields.add((Rectangle) currentNode);
            }
        }
        PC.setFields(PCBoardFields);

        boatCarrier.setFill(new ImagePattern(boatFiveImage));
        boatBattleship.setFill(new ImagePattern(boatFourImage));
        boatCruiser.setFill(new ImagePattern(boatThree2Image));
        boatSubmarine.setFill(new ImagePattern(boatThree1Image));
        boatDestroyer.setFill(new ImagePattern(boatTwoImage));

        ships.add(boatCarrier);
        ships.add(boatBattleship);
        ships.add(boatCruiser);
        ships.add(boatSubmarine);
        ships.add(boatDestroyer);

        ships.forEach(this::setDragListeners);

        playerBoardBounds = playerBoard.localToScene(playerBoard.getBoundsInParent());

        //Liseneri na polja ploca
        setBoardFieldsListeners(PC.getFields());
        //  setBoardFieldsListeners(player.getFields());
        playerBoard.setDisable(true);
        PCBoard.setDisable(true);


        // TODO : pokusati napraviti elegantnijim vracanje brodica na poziciju kada se klikne play again/ da nekako vratim brodice na firstPosition
        // TODO : kada se pogodi da se na tom mjestu napravi X
        // TODO : napraviti kao health slicicu koja ce se mijenjati u skladu sa zdravljem
        // TODO : staviti zvuk
        // TODO : Bug koji se pojavljuje nekada prilikom postavljanja brodica, error da nisu pozicionirani svi iako jesu
        // TODO : ako je polje occupied onda kada se presijece sa tim poljem ne moze se tu postaviti
        // TODO : Bug prilikom igranja, zablokirai kad se treba kliknuti
    }

    public void setBoardFieldsListeners(List<List<Field>> boardFields) {
        boardFields.forEach(rows -> rows.forEach(field -> {
            EventHandler<MouseEvent> mouseEntered = event -> {
                if (field.getColor() == Color.DODGERBLUE)
                    field.setColor(Color.GREEN);
            };
            EventHandler<MouseEvent> mouseExited = event -> {
                if (field.getColor() == Color.GREEN)
                    field.setColor(Color.DODGERBLUE);
            };
            EventHandler<MouseEvent> mouseClicked = event -> {
                PC.playerTurn(field);
                PCBoard.setDisable(true);
                PCHealth.setProgress(PC.getHealth() / 17.);

                player.enemyTurn(strategyOneAI);
                PCBoard.setDisable(false);
                playerHealth.setProgress(player.getHealth() / 17.);
            };

            field.getRectangle().addEventHandler(MouseEvent.MOUSE_ENTERED, mouseEntered);
            // Removing the Green when exit
            field.getRectangle().addEventHandler(MouseEvent.MOUSE_EXITED, mouseExited);
            //kada kliknemo da se sacuva pozicija kliknutog polja
            field.getRectangle().addEventHandler(MouseEvent.MOUSE_CLICKED, mouseClicked);
        }));
    }

    public void setDragListeners(final Rectangle shipRectangle) {
        final Delta dragDelta = new Delta();
        Ship ship = new Ship(shipRectangle.getWidth(), shipRectangle.getLayoutX(), shipRectangle.getLayoutY());
        shipRectangle.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                shipRectangle.toFront();
                shipRectangle.setCursor(Cursor.CLOSED_HAND);

                //zapamti pocetnu poziciju
                if (dragDelta.getFirstLayoutX() == 0) {
                    dragDelta.setFirstLayoutX(shipRectangle.getLayoutX());
                    dragDelta.setFirstLayoutY(shipRectangle.getLayoutY());
                }
                if (isBoatOnBoard(shipRectangle, playerBoardBounds)) {
                    player.removeShip(ship);
                }
                dragDelta.setX(shipRectangle.getLayoutX() - mouseEvent.getSceneX());
                dragDelta.setY(shipRectangle.getLayoutY() - mouseEvent.getSceneY());

                //na desni klik, rotacija
                if (mouseEvent.isSecondaryButtonDown()) {
                    setRotation(shipRectangle);
                    checkShapeIntersection(shipRectangle);
                }
            }
        });
        shipRectangle.setOnMouseReleased(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                shipRectangle.setCursor(Cursor.OPEN_HAND);

                if (!isBoatOnBoard(shipRectangle, playerBoardBounds)) {
                    shipRectangle.setLayoutX(dragDelta.getFirstLayoutX());
                    shipRectangle.setLayoutY(dragDelta.getFirstLayoutY());
                    player.removeShip(ship);
                } else {
                    for (Shape field : playerBoardFields) {
                        if (field.getFill() == Color.GREEN) {
                            ship.setOrientation(shipRectangle.getRotate());
                            ship.setStartY(field.getLayoutY());
                            ship.setStartX(field.getLayoutX());
                            player.addShip(ship);
                            //ovaj if-else je za pravilno snapovanje
                            if (shipRectangle.getRotate() == 0) {
                                shipRectangle.setLayoutY(field.getLayoutY() + 5);
                                shipRectangle.setLayoutX(field.getLayoutX());
                                break;
                            } else {
                                int deviation = outBoard(shipRectangle);
                                shipRectangle.setLayoutX(field.getLayoutX() - deviation + 5);
                                shipRectangle.setLayoutY(field.getLayoutY() + deviation);
                                break;
                            }
                        }
                    }
                }
                for (Shape field : playerBoardFields) {
                    if (field.getFill() == Color.GREEN || field.getFill() == Color.CORAL)
                        field.setFill(Color.DODGERBLUE);
                }
            }
        });
        shipRectangle.setOnMouseDragged(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {

                shipRectangle.setLayoutX(mouseEvent.getSceneX() + dragDelta.getX());
                shipRectangle.setLayoutY(mouseEvent.getSceneY() + dragDelta.getY());
                System.out.println(shipRectangle.getLayoutX());
                System.out.println(shipRectangle.getLayoutY());

                checkShapeIntersection(shipRectangle);
            }
        });
    }

    private void setDodgerblueColor(List<Rectangle> board) {
        for (Rectangle field : board) {
            if (field.getFill() == Color.CORAL)
                field.setFill(Color.DODGERBLUE);
        }
    }

    private void setRotation(Rectangle ship) {
        if (ship.getRotate() == 0)
            ship.setRotate(270);
        else
            ship.setRotate(0);
    }

    private boolean isBoatOnBoard(Rectangle ship, Bounds board) {
        if (ship.getRotate() == 0) {
            return ship.getLayoutX() >= 0 && ship.getLayoutX() <= (board.getMaxX() + 5 - ship.getWidth()) &&
                    ship.getLayoutY() >= 0 && ship.getLayoutY() <= board.getMaxY() + 5 - ship.getHeight();
        } else {
            int outBoard = outBoard(ship);
            //+5 na velicinu ploce da se ne detekltuje odmah cim se dotakne okvir da je izvan ploce brodic
            return ship.getLayoutX() >= -outBoard && ship.getLayoutX() <= (board.getMaxX() + 5 - ship.getHeight() + outBoard) &&
                    ship.getLayoutY() >= outBoard && ship.getLayoutY() <= board.getMaxY() + 5 - ship.getWidth() + outBoard;
        }
    }

    private int outBoard(Rectangle ship) {
        return ((int) ship.getWidth() / 2) - 20;
    }

    // da se ne boje dva reda/kolone ako se presijecaju polja vec samo ako je presjeceno vise od pola polja
    private boolean occupiesOneRow(Rectangle ship, Rectangle field) {
        return ship.getLayoutY() <= field.getLayoutY() + 30 && ship.getLayoutY() > field.getLayoutY() - field.getHeight() + 30;
    }

    private boolean occupiesRightWidth(Rectangle ship, Rectangle field) {
        return (ship.getLayoutX() <= field.getLayoutX() + field.getWidth() / 2);
    }

    private boolean occupiesRightWidth2(Rectangle ship, Rectangle field) {
        return ship.getLayoutX() + ship.getWidth() > field.getLayoutX() + field.getWidth() / 2;
    }

    private boolean occupiesRightHight(Rectangle ship, Rectangle field) {
        int deviation = outBoard(ship);
        return (ship.getLayoutY() - deviation <= field.getLayoutY() + field.getWidth() / 2);
    }

    private boolean occupiesRightHight2(Rectangle ship, Rectangle field) {
        int deviation = outBoard(ship);
        return ship.getLayoutY() - deviation + ship.getWidth() > field.getLayoutY() + field.getWidth() / 2;
    }

    private boolean occupiesOneColumn(Rectangle ship, Rectangle field) {
        int deviation = outBoard(ship);
        return ship.getLayoutX() <= field.getLayoutX() - deviation + 30 && ship.getLayoutX() > field.getLayoutX() - deviation - field.getWidth() + 30;
    }

    private void checkShapeIntersection(Rectangle block) {
        for (Rectangle field : playerBoardFields) {
            Shape intersect = Shape.intersect(block, field);
            if (intersect.getBoundsInLocal().getWidth() != -1) {

                if (block.getRotate() == 0) {
                    if (occupiesOneRow(block, field) && occupiesRightWidth(block, field) &&
                            occupiesRightWidth2(block, field) && isBoatOnBoard(block, playerBoardBounds)) {
                        field.setFill(Color.GREEN);
                    } else if (occupiesOneRow(block, field) && occupiesRightWidth(block, field) &&
                            occupiesRightWidth2(block, field) && !isBoatOnBoard(block, playerBoardBounds))
                        field.setFill(Color.CORAL);
                    else
                        field.setFill(Color.DODGERBLUE);
                } else {
                    if (occupiesOneColumn(block, field) && occupiesRightHight(block, field)
                            && occupiesRightHight2(block, field) && isBoatOnBoard(block, playerBoardBounds))
                        field.setFill(Color.GREEN);
                    else if
                    (occupiesOneColumn(block, field) && occupiesRightHight(block, field)
                                    && occupiesRightHight2(block, field) && !isBoatOnBoard(block, playerBoardBounds))
                        field.setFill(Color.CORAL);
                    else
                        field.setFill(Color.DODGERBLUE);
                }
            } else
                field.setFill(Color.DODGERBLUE);
        }
    }

    private void finalPreparations() {
        playerBoard.setDisable(true);
        PCBoard.setDisable(false);
        ships.forEach(ship -> ship.setDisable(true));
        startButton.setDisable(true);
        randomButton.setDisable(true);
        PC.setRandomShips();
        //besmisleno ako je random kliknuto
        player.setOccupiedFields();

        player.setInitialHealth();
        PC.setInitialHealth();
    }

    private void reset() {
        startButton.setDisable(false);
        randomButton.setDisable(false);
        textArea.setText("");
        for (int i = 0; i < ships.size(); i++) {
            Rectangle ship = ships.get(i);
            ship.setDisable(false);
            ship.setRotate(0);
            ship.setLayoutX(10);
            ship.setLayoutY(50 * i + 600);
        }
        PCBoard.setDisable(true);
        player.resetBoard();
        PC.resetBoard();
        //ovisno koji je AI ukljucen
        strategyOneAI.reset();
        sequenceAI.reset();
        strategyTwoAI.reset();
        playerHealth.setProgress(1);
        PCHealth.setProgress(1);
    }

    public void start(MouseEvent mouseEvent) {
        Alert alert = new Alert(Alert.AlertType.ERROR, "Nisu postavljeni svi brodici na polje!");
        if (player.getShips().size() != 5) {
            alert.showAndWait();
        } else {
            finalPreparations();
        }
    }

    public void playAgain(MouseEvent mouseEvent) {
        reset();
    }

    public void placeShipsRandom(MouseEvent mouseEvent) {
        player.removeOccupied();
        player.setRandomShips();
        ships.forEach(ship -> ship.setDisable(true));
        for (int i = 0; i < 5; i++) {
            Ship ship = player.getShips().get(i);
            Rectangle rectangle = ships.get(i);
            if (ship.getOrientation() == Orientation.HORIZONTAL) {
                rectangle.setRotate(0);
                rectangle.setLayoutY(ship.getStartY() + 5);
                rectangle.setLayoutX(ship.getStartX());
            } else {
                rectangle.setRotate(270);
                int deviation = outBoard(rectangle);
                rectangle.setLayoutX(ship.getStartX() - deviation + 5);
                rectangle.setLayoutY(ship.getStartY() + deviation);
            }
        }
    }

    public void explore(MouseEvent mouseEvent) {
        XYChart.Series random = new XYChart.Series();
        random.setName("Random");
        XYChart.Series sequnece = new XYChart.Series();
        sequnece.setName("Sequence");
        XYChart.Series strategyOne = new XYChart.Series();
        strategyOne.setName("StrategyOne");
        XYChart.Series strategyTwo = new XYChart.Series();
        strategyTwo.setName("StrategyTwo");
        String text = "";
        int hits;
        for (int i = 1; i <= 50; i++) {
            hits = 0;
            Board boardTest = new Board(PCBoardFields);
            boardTest.setRandomShips();
            boardTest.setHealth(17);
            while (boardTest.getHealth() != 0) {
                hits++;
                boardTest.enemyTurnTest(randomAI);
            }
            random.getData().add(new XYChart.Data(i,hits));
        }
        for (int i = 1; i <= 50; i++) {
            hits = 0;
            Board boardTest = new Board(PCBoardFields);
            boardTest.setRandomShips();
            boardTest.setHealth(17);
            while (boardTest.getHealth() != 0) {
                hits++;
                boardTest.enemyTurnTest(sequenceAI);
            }
            sequnece.getData().add(new XYChart.Data(i,hits));
            sequenceAI.reset();
        }
        System.out.println("**************************EXPLORE***************************");
        for (int i = 1; i <= 50; i++) {
            System.out.println("*********************START*****************");
            hits = 0;
            StrategyOneAI strategyOneAI = new StrategyOneAI();
            Board boardTest = new Board(PCBoardFields);
            boardTest.setRandomShips();
            boardTest.setHealth(17);
            while (boardTest.getHealth() != 0) {
                hits++;
                boardTest.enemyTurnTest(strategyOneAI);
            }
            text+=i+". gađanja "+hits+"\n";
            boardTest.resetBoard();
            strategyOne.getData().add(new XYChart.Data<>(i,hits));
            //strategyTwoAI.reset();
        }
        textArea.setText(text);
        /*
        for (int i = 48; i <= 50; i++) {
            System.out.println("*********************START*****************");
            hits = 0;
            StrategyOneAI strategyTwoAI = new StrategyTwoAI();
            Board boardTest = new Board(PCBoardFields);
            boardTest.setRandomShips();
            boardTest.setHealth(17);
            while (boardTest.getHealth() != 0) {
                hits++;
                boardTest.enemyTurnTest(strategyTwoAI);
            }
            text+=i+". gađanja "+hits+"\n";
            boardTest.resetBoard();
            strategyTwo.getData().add(new XYChart.Data<>(i,hits));
            //strategyTwoAI.reset();
        }*/
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("chart.fxml"));
            Parent root = loader.load();
            chartController chartController = loader.getController();
            chartController.transferData(random,sequnece,strategyOne,strategyTwo);

            Stage startStage = new Stage();
            startStage.setTitle("Battleship");
            startStage.setResizable(false);
            startStage.setScene(new Scene(root));
            startStage.showAndWait();
        } catch (IOException error) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Problem " + error.getMessage());
            alert.showAndWait();
        }
    }
}
