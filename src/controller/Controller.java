package controller;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import model.NavState;
import model.User;
import model.chat.Message;
import networking.ClientStream;
import networking.IClient;
import networking.IServer;
import networking.ServerStream;

public class Controller {
	private static final int MIN_USERS = 2; // default min users required to start
	private static final int ROOM_CAPACITY = 6; // default max room capacity

	private static final Pattern PATTERN_NICKNAME = Pattern.compile("^[a-zA-Z0-9]{3,15}$");
	private static final Pattern PATTERN_IP = Pattern.compile("^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");

	private NavState state;

	@FXML private VBox vboxBack;
	@FXML private Button buttonBack;

	// MultiPlayer
	@FXML private VBox vboxMP;
	@FXML private Button buttonC; // button Create
	@FXML private Button buttonJ; // button Join

	// MultiPlayer: Create New Room
	@FXML private VBox vboxCreateRoom;
	@FXML private TextField textFieldNicknameS;
	@FXML private TextFlow labelErrorNicknameS;
	@FXML private Label labelMinRoom;
	@FXML private Label labelMaxRoom;
	@FXML private ImageView buttonIncreaseMinRoom;
	@FXML private ImageView buttonDecreaseMinRoom;
	@FXML private ImageView buttonIncreaseMaxRoom;
	@FXML private ImageView buttonDecreaseMaxRoom;
	private Image arrowUp;
	private Image arrowUpDisabled;
	private Image arrowDown;
	private Image arrowDownDisabled;
	@FXML private CheckBox checkBoxRejoin;
	@FXML private Button buttonCNR; // button Create New Room

	// MultiPlayer: Join Existing Room
	@FXML private VBox vboxJoinRoom;
	@FXML private TextField textFieldNicknameC;
	@FXML private TextFlow labelErrorNicknameC;
	@FXML private TextField textFieldIP;
	@FXML private Label labelErrorIP;
	@FXML private HBox hboxConnection; // hbox connection
	@FXML private Button buttonJER;

	// MultiPlayer: Server
	private IServer server;
	@FXML private VBox vboxServerRoom;
	@FXML private Label labelServerIP;
	@FXML private ListView<HBox> listViewUsersS;
	private ArrayList<Label> listNicknameS;
	private ArrayList<Label> listReadyS;
	private ArrayList<Label> listLabelBan;
	@FXML private Button buttonRoomSettings;
	@FXML private Button buttonStartGame;
	@FXML private Button buttonOpenClose;
	@FXML private Label labelOpenClose;
	@FXML private TextArea textAreaChatS;
	@FXML private TextField textFieldChatS;
	@FXML private Button buttonChatSendS;


	// MultiPlayer: Client
	private IClient client;
	@FXML private VBox vboxClientRoom;
	@FXML private ListView<HBox> listViewUsersC;
	private ArrayList<Label> listNicknameC;
	private ArrayList<Label> listReadyC;
	private ArrayList<ImageView> listImagePlayer;
	@FXML private Button buttonReady;
	@FXML private TextArea textAreaChatC;
	@FXML private TextField textFieldChatC;
	@FXML private Button buttonChatSendC;

	//Game
	@FXML private VBox vboxGame;
	private int wordCounter = 0;
	private int first = 1;
	private int win = 0;
	private double finishX;
    private File saveData;
    ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

    @FXML public Text seconds;
	@FXML private Text programWord;
	@FXML private Text secondProgramWord;
	@FXML private Text thirdProgramWord;
	@FXML private Text fourthProgramWord;
    @FXML private TextField userWord;
    @FXML private ImageView correct;
    @FXML private ImageView car;
    @FXML private ImageView finish;
    @FXML private ImageView wrong;
    @FXML private Button playAgain;

    @FXML
	private void moveCarRight() {
	    car.setLayoutX(car.getLayoutX() + 50);
	}

    ArrayList<String> words = new ArrayList<String>();

    // add words to array list
    public void addToList() {
    	BufferedReader reader;
        try {
            InputStream inputStream = getClass().getResourceAsStream("wordsList");
            if (inputStream != null) {
                reader = new BufferedReader(new InputStreamReader(inputStream));
                String line = reader.readLine();
                while (line != null) {
                    words.add(line);
                    // read next line
                    line = reader.readLine();
                }
                reader.close();
            } else {
                System.err.println("File not found: wordsList");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        Collections.shuffle(words);
    }

    public void initializeGame() {

		playAgain.setVisible(false);
        playAgain.setDisable(true);
        finishX = finish.getLayoutX();
        seconds.setText("60");
        programWord.setText(words.get(wordCounter));
        secondProgramWord.setText(words.get(wordCounter+1));
        thirdProgramWord.setText(words.get(wordCounter+2));
        fourthProgramWord.setText(words.get(wordCounter+3));
        wordCounter++;

//        Date date = new Date();
//        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH-mm-ss");
//        saveData = new File("src/data/"+formatter.format(date).trim()+".txt");

//        try {
//            if (saveData.createNewFile()) {
//                System.out.println("File created: " + saveData.getName());
//            } else {
//                System.out.println("File already exists.");
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

	}

    private int timer = 60;

	Runnable r = new Runnable() {
        @Override
        public void run() {
            if (timer > -1) {
                seconds.setText(String.valueOf(timer));
                timer -= 1;
            }

            else {
                if (timer == -1) {
                	if (win == 0){
                    	userWord.setDisable(true);
                        userWord.setText("Game over. You lose!");
                        timer = -4;
                	} else {
                    	userWord.setDisable(true);
                        userWord.setText("Game over. You win!");
                        timer = -4;
                        System.out.println(timer);
                	}

//                    try {
//                        FileWriter myWriter = new FileWriter(saveData);
//                        myWriter.write(countAll +";");
//                        myWriter.write(counter +";");
//                        myWriter.write(String.valueOf(countAll-counter));
//                        myWriter.close();
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
                }

                if (timer == -4) {
                    playAgain.setVisible(true);
                    playAgain.setDisable(false);
                    executor.shutdown();
                }

                timer -= 1;
            }
        }
    };

    public void backtoMenu(ActionEvent event){
    	this.vboxGame.setVisible(false);
    	this.vboxMP.setVisible(true);
    	this.vboxMP.setDisable(false);
    }

    Runnable fadeCorrect = new Runnable() {
        @Override
        public void run() {
            correct.setOpacity(0);
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            correct.setOpacity(50);
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            correct.setOpacity(100);
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            correct.setOpacity(0);
        }
    };

    Runnable fadeWrong = new Runnable() {
        @Override
        public void run() {
            wrong.setOpacity(0);
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            wrong.setOpacity(50);
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            wrong.setOpacity(100);
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            wrong.setOpacity(0);
        }
    };



    private int countAll = 0;
    private int counter = 0;


    public void startType(KeyEvent ke) {
        // only gets called once
        if (first == 1) {
            first = 0;
            executor.scheduleAtFixedRate(r, 0, 1, TimeUnit.SECONDS);

        }


        if (ke.getCode().equals(KeyCode.ENTER)) {

            String s = userWord.getText().trim();
            String real = programWord.getText();
            countAll++;

            // if correct
            if (s.equals(real)) {
                counter++;
                //wordsPerMin.setText(String.valueOf(counter));
                Thread t = new Thread(fadeCorrect);
                t.start();
                moveCarRight();

                System.out.println(finishX + "vs" + car.getLayoutX());

                if ((finish.getLayoutX() - 101) <= car.getLayoutX()){
                    win = 1;
                	timer = -1;
                }
            }

            else {
                Thread t = new Thread(fadeWrong);
                t.start();
            }
            userWord.setText("");
            //accuracy.setText(String.valueOf(Math.round((counter*1.0/countAll)*100)));
            programWord.setText(words.get(wordCounter));
            secondProgramWord.setText(words.get(wordCounter+1));
            thirdProgramWord.setText(words.get(wordCounter+2));
            fourthProgramWord.setText(words.get(wordCounter+3));
            wordCounter++;
        }

    }


	private int connectedUsers;

	private SimpleDateFormat tformatter;

	public Controller() {}

	public void initialize()
	{
		this.state = NavState.MULTIPLAYER;

		this.vboxBack.setVisible(true);
		this.vboxMP.setVisible(true);
		this.vboxCreateRoom.setVisible(false);
		this.vboxJoinRoom.setVisible(false);
		this.vboxServerRoom.setVisible(false);
		this.vboxClientRoom.setVisible(false);
		this.vboxGame.setVisible(false);

		this.tformatter = new SimpleDateFormat("[HH:mm:ss]");
		this.showConnectingBox(false);

		this.buttonCNR.setDisable(true);
		this.buttonJER.setDisable(true);
		this.labelErrorIP.setVisible(false);

		this.listNicknameC = new ArrayList<Label>();
		this.listNicknameS = new ArrayList<Label>();
		this.listReadyC = new ArrayList<Label>();
		this.listReadyS = new ArrayList<Label>();
		this.listImagePlayer = new ArrayList<ImageView>();
		this.listLabelBan = new ArrayList<Label>();

		this.labelMinRoom.setText("" + MIN_USERS);
		this.labelMaxRoom.setText("" + ROOM_CAPACITY);

		this.arrowUp = new Image(this.getClass().getResource("/resources/icon-arrow-up.png").toString());
		this.arrowUpDisabled = new Image(this.getClass().getResource("/resources/icon-arrow-up-disabled.png").toString());
		this.arrowDown = new Image(this.getClass().getResource("/resources/icon-arrow-down.png").toString());
		this.arrowDownDisabled = new Image(this.getClass().getResource("/resources/icon-arrow-down-disabled.png").toString());

		this.buttonDecreaseMinRoom.setDisable(true);
		this.buttonIncreaseMaxRoom.setDisable(true);
		this.buttonIncreaseMinRoom.setImage(this.arrowUp);
		this.buttonDecreaseMinRoom.setImage(this.arrowDownDisabled);
		this.buttonIncreaseMaxRoom.setImage(this.arrowUpDisabled);
		this.buttonDecreaseMaxRoom.setImage(this.arrowDown);

		// set automatic chat scrolling to bottom
		this.textAreaChatS.textProperty().addListener(new ChangeListener<Object>() {
		    @Override
		    public void changed(ObservableValue<?> observable, Object oldValue,
		            Object newValue) {
		    	textAreaChatS.setScrollTop(Double.MAX_VALUE); //this will scroll to the bottom
		    }
		});
		this.textAreaChatC.textProperty().addListener(new ChangeListener<Object>() {
		    @Override
		    public void changed(ObservableValue<?> observable, Object oldValue,
		            Object newValue) {
		    	textAreaChatC.setScrollTop(Double.MAX_VALUE); //this will scroll to the bottom
		    }
		});

		// popolate the ListView with HBox and set them not visible
		for(int i = 0; i < ROOM_CAPACITY; i++)
		{
			// hbox client
			HBox hbox = new HBox();
			hbox.setPrefSize(280, 25);
			hbox.setSpacing(10);
			hbox.setVisible(false);
			// nickname client
			Label l = new Label("");
			l.setPrefWidth(200);
			l.setTextFill(Paint.valueOf("white"));
			hbox.getChildren().add(l);
			this.listNicknameC.add(l);
			// ready client
			l = new Label("");
			l.setPrefSize(25, 25);
			l.setStyle("-fx-background-color: red");
			l.setVisible(i == 0 ? false : true);
			hbox.getChildren().add(l);
			this.listReadyC.add(l);
			// identifier image
			ImageView iv = new ImageView(new Image(this.getClass().getResource("/resources/icon-user.png").toString()));
			iv.resize(25, 25);
			hbox.getChildren().add(iv);
			this.listImagePlayer.add(iv);

			this.listViewUsersC.getItems().add(hbox);

			// hbox server
			hbox = new HBox();
			hbox.setPrefSize(300, 25);
			hbox.setSpacing(10);
			hbox.setVisible(false);
			// nickname server
			l = new Label("");
			l.setPrefWidth(180);
			l.setTextFill(Paint.valueOf("white"));
			hbox.getChildren().add(l);
			this.listNicknameS.add(l);
			// ready server
			l = new Label("");
			l.setPrefSize(25, 25);
			l.setStyle(i == 0 ? "-fx-background-color: lime" : "-fx-background-color: red");
			l.setTooltip(new Tooltip("is ready?"));
			l.setVisible(i == 0 ? false : true);
			hbox.getChildren().add(l);
			this.listReadyS.add(l);

			this.listViewUsersS.getItems().add(hbox);
		}

		connectedUsers = 0;
	}
	// Multiplayer callbacks
	@FXML public void goBack(ActionEvent event)
	{
		switch(this.state)
		{
			case MULTIPLAYER:
			{
				// nothing

				break;
			}
			case MP_CREATE:
			{
				this.vboxCreateRoom.setVisible(false);
				this.vboxMP.setVisible(true);

				this.state = NavState.MULTIPLAYER;

				break;
			}
			case MP_JOIN:
			{
				this.vboxJoinRoom.setVisible(false);
				this.vboxMP.setVisible(true);

				this.state = NavState.MULTIPLAYER;

				break;
			}
			case MP_SERVER:
			{
				this.closeConnection();
				this.vboxServerRoom.setVisible(false);
				this.vboxMP.setVisible(true);

				this.state = NavState.MULTIPLAYER;

				break;
			}
			case MP_CLIENT:
			{
				this.closeConnection();
				this.vboxClientRoom.setVisible(false);
				this.vboxMP.setVisible(true);

				this.state = NavState.MULTIPLAYER;

				break;
			}
			default:
			{
				break;
			}
		}
	}
	@FXML public void selectCNR(ActionEvent event)
	{
		this.vboxMP.setVisible(false);
		this.vboxCreateRoom.setVisible(true);

		// reset CNR fields?

		this.state = NavState.MP_CREATE;
	}
	@FXML public void selectJER(ActionEvent event)
	{
		this.vboxMP.setVisible(false);
		this.vboxJoinRoom.setVisible(true);

		this.hboxConnection.setVisible(false);

		// reset JER fields?

		this.state = NavState.MP_JOIN;
	}

	// MultiPlayer: Create New Room callbacks
	@FXML public void validateNicknameS()
	{
		// nickname OK
		if(this.checkNickname(this.textFieldNicknameS.getText()))
		{
			this.buttonCNR.setDisable(false);
			this.labelErrorNicknameS.setVisible(false);
			this.textFieldNicknameS.setStyle("-fx-border-width: 0px; -fx-focus-color: #039ED3;");
		}
		// nickname NOT
		else
		{
			this.buttonCNR.setDisable(true);
			this.labelErrorNicknameS.setVisible(true);
			this.textFieldNicknameS.setStyle("-fx-text-box-border: red; -fx-focus-color: red;");
		}
	}
	@FXML public void increaseMinRoom(MouseEvent event)
	{
		this.buttonDecreaseMinRoom.setDisable(false);
		this.buttonDecreaseMinRoom.setImage(this.arrowDown);

		int min = Integer.parseInt(this.labelMinRoom.getText());
		int max = Integer.parseInt(this.labelMaxRoom.getText());

		this.labelMinRoom.setText("" + ++min);

		if(min == max)
		{
			this.buttonIncreaseMinRoom.setDisable(true);
			this.buttonIncreaseMinRoom.setImage(this.arrowUpDisabled);
			this.buttonDecreaseMaxRoom.setDisable(true);
			this.buttonDecreaseMaxRoom.setImage(this.arrowDownDisabled);
		}
	}
	@FXML public void decreaseMinRoom(MouseEvent event)
	{
		this.buttonIncreaseMinRoom.setDisable(false);
		this.buttonIncreaseMinRoom.setImage(this.arrowUp);
		this.buttonDecreaseMaxRoom.setDisable(false);
		this.buttonDecreaseMaxRoom.setImage(this.arrowDown);

		int value = Integer.parseInt(this.labelMinRoom.getText());
		if(value != MIN_USERS)
		{
			this.labelMinRoom.setText("" + --value);
			if(value == MIN_USERS)
			{
				this.buttonDecreaseMinRoom.setDisable(true);
				this.buttonDecreaseMinRoom.setImage(this.arrowDownDisabled);
			}
		}
	}
	@FXML public void increaseMaxRoom(MouseEvent event)
	{
		this.buttonIncreaseMinRoom.setDisable(false);
		this.buttonIncreaseMinRoom.setImage(this.arrowUp);
		this.buttonDecreaseMaxRoom.setDisable(false);
		this.buttonDecreaseMaxRoom.setImage(this.arrowDown);

		int value = Integer.parseInt(this.labelMaxRoom.getText());
		if(value != ROOM_CAPACITY)
		{
			this.labelMaxRoom.setText("" + ++value);
			if(value == ROOM_CAPACITY)
			{
				this.buttonIncreaseMaxRoom.setDisable(true);
				this.buttonIncreaseMaxRoom.setImage(this.arrowUpDisabled);
			}
		}
	}
	@FXML public void decreaseMaxRoom(MouseEvent event)
	{
		this.buttonIncreaseMaxRoom.setDisable(false);
		this.buttonIncreaseMaxRoom.setImage(this.arrowUp);

		int min = Integer.parseInt(this.labelMinRoom.getText());
		int max = Integer.parseInt(this.labelMaxRoom.getText());

		this.labelMaxRoom.setText("" + --max);

		if(min == max)
		{
			this.buttonIncreaseMinRoom.setDisable(true);
			this.buttonIncreaseMinRoom.setImage(this.arrowUpDisabled);
			this.buttonDecreaseMaxRoom.setDisable(true);
			this.buttonDecreaseMaxRoom.setImage(this.arrowDownDisabled);
		}
	}
	@FXML public void createNewRoom(ActionEvent event)
	{
		if(!this.checkNickname(this.textFieldNicknameS.getText()))
		{
			this.showAlert(AlertType.ERROR, "Invalid nickname", "The nickname bust be from 3 to 15 alphanumeric char long.");
			this.buttonCNR.setDisable(true);
			this.labelErrorNicknameS.setVisible(true);
			this.textFieldNicknameS.setStyle("-fx-text-box-border: red; -fx-focus-color: red;");
			return;
		}

		this.setServerAddress();
		this.textAreaChatS.setText(this.getCurrentTimestamp() + " " + this.textFieldNicknameS.getText() + " created the room");

		// create new room -> start server (if OK switch to Server Room View)
		this.server = new ServerStream(this, this.textFieldNicknameS.getText(), Integer.parseInt(this.labelMinRoom.getText()), Integer.parseInt(this.labelMaxRoom.getText()), this.checkBoxRejoin.isSelected());
		this.client = null;

		// reset the user list
		this.resetList();

		// reset buttons
		this.buttonStartGame.setDisable(true);
		this.buttonOpenClose.setText("Open");
		this.labelOpenClose.setStyle("-fx-background-color: lime");

		// set the first list element (the server) to visibile
		this.listNicknameS.get(0).setText(this.textFieldNicknameS.getText());
		this.listViewUsersS.getItems().get(0).setVisible(true);

		this.connectedUsers = 1;

	}

	// MultiPlayer: Join Existing Room callbacks
	@FXML public void validateNicknameAddressC()
	{
		// nickname OK & address OK (or empty)
		if(this.checkNickname(this.textFieldNicknameC.getText()) && (this.checkIP(this.textFieldIP.getText()) || this.textFieldIP.getText().isEmpty()))
		{
			this.buttonJER.setDisable(false);
			this.labelErrorNicknameC.setVisible(false);
			this.labelErrorIP.setVisible(false);
			// reset borders & focus (nickname)
			this.textFieldNicknameC.setStyle("-fx-border-width: 0px; -fx-focus-color: #039ED3;");
			// reset borders & focus (address)
			this.textFieldIP.setStyle("-fx-border-width: 0px; -fx-focus-color: #039ED3;");
		}
		// nickname OK & address NOT (nor empty)
		else if(this.checkNickname(this.textFieldNicknameC.getText()) && !(checkIP(this.textFieldIP.getText()) || this.textFieldIP.getText().isEmpty()))
		{
			this.buttonJER.setDisable(true);
			this.labelErrorNicknameC.setVisible(false);
			this.labelErrorIP.setVisible(true);
			// reset borders & focus (nickname)
			this.textFieldNicknameC.setStyle("-fx-border-width: 0px; -fx-focus-color: #039ED3;");
			// reset borders & focus (address)
			this.textFieldIP.setStyle("-fx-text-box-border: red; -fx-focus-color: red;");
		}
		// nickname NOT & address OK (or empty)
		else if(!this.checkNickname(this.textFieldNicknameC.getText()) && (checkIP(this.textFieldIP.getText()) || this.textFieldIP.getText().isEmpty()))
		{
			this.buttonJER.setDisable(true);
			this.labelErrorNicknameC.setVisible(true);
			this.labelErrorIP.setVisible(false);
			// red borders & focus (nickname)
			this.textFieldNicknameC.setStyle("-fx-text-box-border: red; -fx-focus-color: red;");
			// reset borders & focus (address)
			this.textFieldIP.setStyle("-fx-border-width: 0px; -fx-focus-color: #039ED3;");
		}
		// nickname NOT & address NOT (nor empty)
		else
		{
			this.buttonJER.setDisable(true);
			this.labelErrorNicknameC.setVisible(true);
			this.labelErrorIP.setVisible(true);
			// red borders & focus (nickname)
			this.textFieldNicknameC.setStyle("-fx-text-box-border: red; -fx-focus-color: red;");
			// red borders & focus (address)
			this.textFieldIP.setStyle("-fx-text-box-border: red; -fx-focus-color: red;");

		}
	}
	@FXML public void joinExistingRoom(ActionEvent event)
	{
		if(!this.checkNickname(this.textFieldNicknameC.getText()))
		{
			this.showAlert(AlertType.ERROR, "Invalid nickname", "The nickname bust be from 3 to 15 alphanumeric char long.");
			this.buttonJER.setDisable(true);
			this.labelErrorNicknameC.setVisible(true);
			this.textFieldNicknameC.setStyle("-fx-text-box-border: red; -fx-focus-color: red;");
			return;
		}
		if(!this.checkIP(this.textFieldIP.getText()) && !this.textFieldIP.getText().isEmpty())
		{
			this.showAlert(AlertType.ERROR, "Invalid IP Address", "The address must be X.X.X.X or empty (localhost).");
			this.buttonJER.setDisable(true);
			this.labelErrorIP.setVisible(true);
			this.textFieldIP.setStyle("-fx-text-box-border: red; -fx-focus-color: red;");
			return;
		}

		// show loading box
		this.showConnectingBox(true);

		// reset ready button
		this.buttonReady.setText("Not ready");
		this.buttonReady.setStyle("-fx-background-color: red");

		// reset textArea
		this.textAreaChatC.setText("");

		// connect to existing room -> start client (if OK switch to Client Room View)
		this.client = new ClientStream(this, this.textFieldIP.getText(), 9001, this.textFieldNicknameC.getText());
		this.server = null;
	}


	@FXML public void toggleOpenClose(ActionEvent event)
	{
		// close the room
		if(this.buttonOpenClose.getText().equalsIgnoreCase("Open"))
		{
			this.buttonOpenClose.setText("Closed");
			this.labelOpenClose.setStyle("-fx-background-color: red");
		}
		// open the room
		else
		{
			this.buttonOpenClose.setText("Open");
			this.labelOpenClose.setStyle("-fx-background-color: lime");
		}
	}
	@FXML public void sendMessageS(ActionEvent event)
	{
		String msg = this.textFieldChatS.getText();
		if(!msg.isEmpty() && !msg.isEmpty())
			this.server.sendChatMessage(msg);
		this.textFieldChatS.setText("");
	}
	@FXML public void enterChatHandleS(KeyEvent event)
	{
		if(this.state == NavState.MP_SERVER && event.getCode().equals(KeyCode.ENTER))
		{
			String msg = this.textFieldChatS.getText();
			if(!msg.isEmpty() && !msg.isEmpty())
				this.server.sendChatMessage(msg);
			this.textFieldChatS.setText("");
		}
	}
	@FXML public void startGame()
	{
		System.out.println("Start game");
		this.vboxGame.setVisible(true); //temp
		addToList();

		this.initializeGame();
		this.server.sendStart();
	}


	// MultiPlayer: Client callbacks
	@FXML public void toggleReady(ActionEvent event)
	{
		if(this.buttonReady.getText().equalsIgnoreCase("Ready"))
		{
			this.buttonReady.setText("Not ready");
			this.buttonReady.setStyle("-fx-background-color: red");
			this.client.sendReady(false);
			this.updateReady(this.textFieldNicknameC.getText(), false);
		}
		else
		{
			this.buttonReady.setText("Ready");
			this.buttonReady.setStyle("-fx-background-color: lime");
			this.client.sendReady(true);
			this.updateReady(this.textFieldNicknameC.getText(), true);
		}
		// TO-DO: set a 5 sec timer that disables the button, so that users can't spam the toggle
	}
	@FXML public void sendMessageC(ActionEvent event)
	{
		String msg = this.textFieldChatC.getText();
		if(!msg.isEmpty() && !msg.isEmpty())
			this.client.sendChatMessage(msg);
		this.textFieldChatC.setText("");
	}
	@FXML public void enterChatHandleC(KeyEvent event)
	{
		if(this.state == NavState.MP_CLIENT && event.getCode().equals(KeyCode.ENTER))
		{
			String msg = this.textFieldChatC.getText();
			if(!msg.isEmpty() && !msg.isEmpty())
				this.client.sendChatMessage(msg);
			this.textFieldChatC.setText("");
		}
	}

	// utilities
	private boolean checkNickname(String text)
	{
		// if OK return true
		return PATTERN_NICKNAME.matcher(text).matches() ? true : false;
	}
	private boolean checkIP(String text)
	{
		// if OK return true
		return PATTERN_IP.matcher(text).matches() ? true : false;
	}
	public void switchToMP()
	{
		if(this.state == NavState.MP_CLIENT)
		{
			this.vboxClientRoom.setVisible(false);
			this.vboxMP.setVisible(true);

			this.state = NavState.MULTIPLAYER;
		}
		else if (this.state == NavState.MP_SERVER)
		{
			this.vboxServerRoom.setVisible(false);
			this.vboxMP.setVisible(true);

			this.state = NavState.MULTIPLAYER;
		}
	}
	public void switchToServerRoom()
	{
		this.vboxCreateRoom.setVisible(false);
		this.vboxServerRoom.setVisible(true);


		this.state = NavState.MP_SERVER;
	}
	public void switchToClientRoom()
	{
		this.vboxJoinRoom.setVisible(false);
		this.vboxClientRoom.setVisible(true);

		this.state = NavState.MP_CLIENT;
	}
	public void showAlert(AlertType aType, String header, String content)
	{
		Platform.runLater(() -> {
			Alert a = new Alert(aType);
			a.setTitle("Information Dialog");
			a.setHeaderText(header);
			a.setContentText(content);
			a.show();
		});
	}
	private void setServerAddress()
	{
		try(final DatagramSocket socket = new DatagramSocket()) {
			socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
			String privateIP = socket.getLocalAddress().getHostAddress();
			this.labelServerIP.setText("Private Server IP address: " + privateIP);
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}
	public void showConnectingBox(boolean value)
	{
		this.hboxConnection.setVisible(value);
	}
	public boolean isRoomOpen()
	{
		return this.buttonOpenClose.getText().equalsIgnoreCase("Open") ? true : false;
	}
	public String getCurrentTimestamp()
	{
		Date date = new Date(System.currentTimeMillis());
		String timestamp = this.tformatter.format(date);

		return timestamp;
	}
	public void addToTextArea(String text)
	{
		// client
		if(this.state == NavState.MP_CLIENT)
		{
			if(this.textAreaChatC.getText().isEmpty())
				this.textAreaChatC.setText(text);
			else this.textAreaChatC.appendText("\n" + text);
		}
		// server
		else if(this.state == NavState.MP_SERVER)
		{
			this.textAreaChatS.appendText("\n" + text);
		}
	}
	public void addToTextArea(Message message)
	{
		this.addToTextArea(message.getTimestamp() + " " + message.getNickname() + ": " + message.getContent());
	}
	public void updateReady(String nickname, boolean ready)
	{
		if(this.state == NavState.MP_CLIENT)
		{
			for(int i = 0; i < this.listViewUsersC.getItems().size(); i++)
			{
				if(nickname.equals(this.listNicknameC.get(i).getText()))
				{
					this.listReadyC.get(i).setStyle(ready ? "-fx-background-color: lime" : "-fx-background-color: red");
					break;
				}
			}
		}
		else if(this.state == NavState.MP_SERVER)
		{
			for(int i = 0; i < this.listViewUsersS.getItems().size(); i++)
			{
				if(nickname.equals(this.listNicknameS.get(i).getText()))
				{
					this.listReadyS.get(i).setStyle(ready ? "-fx-background-color: lime" : "-fx-background-color: red");
					break;
				}
			}
		}
	}
	public void resetList()
	{
		if(this.state == NavState.MP_CLIENT)
		{
			Platform.runLater(() -> {
				for(int i = 0; i < ROOM_CAPACITY; i++)
				{
					this.listViewUsersC.getItems().get(i).setVisible(false);
					this.listNicknameC.get(i).setText("");
					this.listReadyC.get(i).setStyle("-fx-background-color: red");
					this.listImagePlayer.get(i).setVisible(false);
				}
			});
		}
		else if(this.state == NavState.MP_SERVER)
		{
			for(int i = 0; i < ROOM_CAPACITY; i++)
			{
				this.listViewUsersS.getItems().get(i).setVisible(false);
				this.listNicknameS.get(i).setText("");
				this.listReadyS.get(i).setStyle("-fx-background-color: red");
			}
		}
	}
	public void addUser(User u)
	{
		Platform.runLater(() -> {
			if(this.state == NavState.MP_CLIENT)
			{
				this.listNicknameC.get(this.connectedUsers).setText(u.getNickname());
				this.listViewUsersC.getItems().get(this.connectedUsers).setVisible(true);
				this.connectedUsers++;
			}
			else if(this.state == NavState.MP_SERVER)
			{
				this.listNicknameS.get(this.connectedUsers).setText(u.getNickname());
				this.listViewUsersS.getItems().get(this.connectedUsers).setVisible(true);
				this.connectedUsers++;

				this.buttonStartGame.setDisable(true); // when a new user connects it's always not ready
			}
		});

	}
	public void removeUser(String nickname)
	{
		Platform.runLater(() -> {
			boolean found = false;
			if(this.state == NavState.MP_CLIENT)
			{
				// NB: we have to move by one position back every user, to fill the empty space left by the removed one
				for(int i = 1; i < this.connectedUsers; i++)
				{
					if(found)
					{
						// we move every entry up by 1, overriding the one to remove
						this.listNicknameC.get(i - 1).setText(this.listNicknameC.get(i).getText());
						this.listReadyC.get(i - 1).setStyle(this.listReadyC.get(i).getStyle());
						this.listImagePlayer.get(i - 1).setVisible(this.listImagePlayer.get(i).isVisible());
					}
					if(this.listNicknameC.get(i).getText().equals(nickname))
						found = true;
				}
				// we hide the last entry
				this.listViewUsersC.getItems().get(this.connectedUsers - 1).setVisible(false);
				this.listNicknameC.get(this.connectedUsers - 1).setText("");
				this.listReadyC.get(this.connectedUsers - 1).setStyle("-fx-background-color: red");
				this.listImagePlayer.get(this.connectedUsers - 1).setVisible(false);
				this.connectedUsers--;
			}
			else if(this.state == NavState.MP_SERVER)
			{
				// NB: we have to move by one position back every user, to fill the empty space left by the removed one
				for(int i = 1; i < this.connectedUsers; i++)
				{
					if(found)
					{
						// we move every entry up by 1, overriding the one to remove
						this.listNicknameS.get(i - 1).setText(this.listNicknameS.get(i).getText());
						this.listReadyS.get(i - 1).setStyle(this.listReadyS.get(i).getStyle());
					}
					if(this.listNicknameS.get(i).getText().equals(nickname))
						found = true;
				}
				// we hide the last entry
				this.listViewUsersS.getItems().get(this.connectedUsers - 1).setVisible(false);
				this.listNicknameS.get(this.connectedUsers - 1).setText("");
				this.listReadyS.get(this.connectedUsers - 1).setStyle("-fx-background-color: red");
				this.connectedUsers--;

				this.buttonStartGame.setDisable(!this.server.checkCanStartGame());
			}
		});
	}
	public void updateUserList(List<User> users)
	{
		Platform.runLater(() -> {
			if(this.state == NavState.MP_CLIENT)
			{
				for(int i = 0; i < users.size(); i++)
				{
					User u = users.get(i);
					this.listNicknameC.get(i).setText(u.getNickname());
					this.listViewUsersC.getItems().get(i).setVisible(true);
					this.listReadyC.get(i).setStyle(u.isReady() ? "-fx-background-color: lime" : "-fx-background-color: red");
					this.listImagePlayer.get(i).setVisible(this.textFieldNicknameC.getText().equals(users.get(i).getNickname()) ? true : false);
				}
				this.connectedUsers = users.size();
			}
			else if(this.state == NavState.MP_SERVER)
			{
				// for the moment it's never used from the server
			}
		});
	}
	public void enableStartGame(boolean value)
	{
		this.buttonStartGame.setDisable(!value);
	}
	public void closeConnection()
	{
		if(this.state == NavState.MP_CLIENT)
		{
			this.client.sendClose();
			this.client = null;
		}
		else if(this.state == NavState.MP_SERVER)
		{
			this.server.sendClose();
			this.server = null;
		}
    }

	private void setBinAnimationOn(MouseEvent event)
	{
		ImageView iv = (ImageView) ((Label) event.getTarget()).getChildrenUnmodifiable().get(0);
		iv.setImage(new Image(this.getClass().getResource("/resources/icon-trash-bin-animated.gif").toString()));
	}
	private void setBinAnimationOff(MouseEvent event)
	{
		ImageView iv = (ImageView) ((Label) event.getTarget()).getChildrenUnmodifiable().get(0);
		iv.setImage(new Image(this.getClass().getResource("/resources/icon-trash-bin.png").toString()));
	}
}
