package com.progark.group2.wizardrumble.states;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.progark.group2.wizardrumble.network.NetworkController;
import com.progark.group2.wizardrumble.states.resources.UIButton;

import java.io.IOException;

import static com.progark.group2.wizardrumble.Application.HEIGHT;
import static com.progark.group2.wizardrumble.Application.WIDTH;


/** The main menu that is shown when the game is launched. The listeners must be added individually
 * to the buttons that are created by the parent class MenuState.
 */
public class MainMenuState extends State {

    private NetworkController network;

    private Table table;
    private Label.LabelStyle titleStyle;
    private Texture backgroundImage;

    private static MainMenuState instance = null;

    private final String title = "Wizard Rumble";
    private int playersOnline = 0;


    //TODO Remove sout and uncomment method calls in the button listeners
    MainMenuState() throws IOException {
        super(GameStateManager.getInstance());
        initialize();

    }

    public static MainMenuState getInstance() throws IOException {
        if (instance == null) {
            instance = new MainMenuState();
        }
        return instance;
    }

    private void startGame() throws IOException {
        GameStateManager.getInstance().set(new LobbyState(gameStateManager));
    }

    private void openSettings(){
        GameStateManager.getInstance().push(MainMenuSettings.getInstance());
    }

    @Override
    public void handleInput() {

    }

    @Override
    public void update(float dt) {

        try {
            playersOnline = NetworkController.getInstance().getPlayersOnlineCount();
        } catch (IOException e) {
            e.printStackTrace();
        }




    }

    @Override

    public void render(SpriteBatch spriteBatch) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        spriteBatch.begin();
        spriteBatch.draw(backgroundImage, 0, 0);
        spriteBatch.end();

        table.clear();
        // Title
        BitmapFont font = new BitmapFont();
        font.setColor(Color.WHITE);
        this.titleStyle = new Label.LabelStyle(font, font.getColor());

        Label label = new Label(this.title, titleStyle);
        label.setAlignment(Align.center);
        label.setSize((float)Gdx.graphics.getWidth()/4, (float)Gdx.graphics.getHeight()/8);
        table.add(label).size((float)Gdx.graphics.getWidth()/4, (float)Gdx.graphics.getHeight()/8);
        table.row();
        label = new Label("Players online: " + playersOnline, titleStyle);
        label.setAlignment(Align.center);
        label.setSize((float)Gdx.graphics.getWidth()/4, (float)Gdx.graphics.getHeight()/12);
        table.add(label).size((float)Gdx.graphics.getWidth()/4, (float)Gdx.graphics.getHeight()/12);
        table.row();

        // startButton
        Stack startButton = new UIButton(new Texture("UI/blue_button00.png"), "Start").getButton();
        startButton.addListener(new InputListener(){
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                return true;
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {

                try {
                    network.requestGameCreation();
                    startGame();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        this.table.add(startButton).pad(10f);
        this.table.row();

        // exitButton
        Stack exitButton = new UIButton(new Texture("UI/blue_button00.png"), "Exit").getButton();
        exitButton.addListener(new InputListener(){
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                return true;
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                onBackButtonPress();
            }
        });
        this.table.add(exitButton).pad(10f);
        this.table.row();

        stage.addActor(table);

        stage.draw();
    }

    @Override
    public void dispose() {
        stage.dispose();
    }

    @Override
    public void onBackButtonPress() {
        Gdx.app.exit();
        System.exit(0);
    }

    /**
     * Initialize stage, input, table and font for title.
     */
    private void initialize() throws IOException {
        // Getting NetworkController
        network = NetworkController.getInstance();
        backgroundImage = new Texture("background.png");

        this.stage = new Stage(new FitViewport(WIDTH, HEIGHT));
        Gdx.input.setInputProcessor(stage);

        // Layout styling
        this.table = new Table();
        table.setFillParent(true);
        //table.setDebug(true);
        table.center();


    }
}
