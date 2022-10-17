package com.thestbar.raycasting;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

public class MainScreen implements Screen {
    private final RayCasting game;
//    private final int HEIGHT = 1080;
//    private final int WIDTH = HEIGHT * 16 / 9;
    private OrthographicCamera camera;

    // Viewport used for game world
    private FitViewport fitViewport;

    // Viewport used for the GUI
    private ScreenViewport screenViewport;

    // Stage used for drawing GUI
    private Stage stage;

    private final int GRID_WIDTH = 50;
    private final int GRID_HEIGHT = 30;

    private final int VIEWPORT_SIZE = 1200;

    private Label debugLabel;
    private Circle playerCollider;
    private final float playerMovementSpeed = 300f;

    private float playerDirectionAngle = 90f;

    private final float playerRotationSpeed = 300f;

    private final float multiplierOfFaceVector = 1000f;


    MainScreen(RayCasting game) {
        this.game = game;

        // Initialize the camera
        camera = new OrthographicCamera();

        // Initialize fit viewport and set the camera to it
        fitViewport = new FitViewport(VIEWPORT_SIZE, (float)VIEWPORT_SIZE * GRID_HEIGHT / GRID_WIDTH, camera);

        // Initialize screen viewport and set the camera to it
        screenViewport = new ScreenViewport();

        // Initialize Stage. Stage needs viewport and batch to work
        stage = new Stage(screenViewport, game.batch);
        Gdx.input.setInputProcessor(stage);

        Table root = new Table();
        root.setFillParent(true);
        root.pad(10);
        stage.addActor(root);

        Table table = new Table();
        root.add(table).growX().growY();

        table.defaults().space(5);

        debugLabel = new Label("Debug Label\n\t" + "Viewport Width: " + fitViewport.getScreenWidth() +
                "\n\tViewport Height: " + fitViewport.getScreenHeight() + "\n\tAspect Ratio: ", game.skin);

        table.add(debugLabel).expandX().left().expandY().top();

        // Initialize player collider (circle)
        float playerRadius = fitViewport.getWorldHeight() / (fitViewport.getWorldHeight() * 0.1f);
        playerCollider = new Circle(0, 0, playerRadius);

        // Initialize tile colliders
    }

    @Override
    public void show() {

    }

    @Override
    public void render(float delta) {
        // Clear the screen with the background colo
        // on this occasion the background color is black
        ScreenUtils.clear(0.1f, 0.1f, 0.1f, 1);

        // Get player input
        input(delta);

        // Apply viewport for game world
        fitViewport.apply();

        // Tell camera to update its matrices
        camera.update();

        // Tell the sprite batch to render in the
        // coordinate system specified by the viewport
        // of the world's coordinate system
        game.batch.setProjectionMatrix(fitViewport.getCamera().combined);

        // Create new batch and start drawing the game world
        game.batch.begin();

        // Draw the grid system
        game.drawer.setColor(Color.LIGHT_GRAY);
        drawGridSystem();

        // Draw player
        game.drawer.setColor(Color.WHITE);
        drawPlayer();

        game.batch.end();

        // Calculate debug label
//        debugLabel.setText("Debug Label\n\t" + "Viewport Width: " + fitViewport.getScreenWidth() +
//                "\n\tViewport Height: " + fitViewport.getScreenHeight() + "\n\tViewport Aspect Ratio: " +
//                1.0f * fitViewport.getScreenWidth() / fitViewport.getScreenHeight());
        debugLabel.setText("Grid width = " + GridSetup.GRID_WIDTH + "\nGrid height = " + GridSetup.GRID_HEIGHT);

        // Apply viewport for GUI
        screenViewport.apply();

        // Draw GUI
        stage.act();
        stage.draw();
    }

    void input(float deltaTime) {
        if(Gdx.input.isKeyPressed(Input.Keys.W)) {
            float factorOfMovementOnYAxis = (float)Math.sin(Math.toRadians(playerDirectionAngle));
            float factorOfMovementOnXAxis = (float)Math.cos(Math.toRadians(playerDirectionAngle));

            // Normalize input
            float newY = factorOfMovementOnYAxis * playerMovementSpeed * deltaTime + playerCollider.y;

            float newX = factorOfMovementOnXAxis * playerMovementSpeed * deltaTime + playerCollider.x;
            float heightLimit = fitViewport.getWorldHeight() / 2f * (1 - 1 / (2f * GRID_HEIGHT)) -
                    3f * playerCollider.radius;
            float widthLimit = fitViewport.getWorldWidth() / 2f * (1 - 1 / (2f * GRID_WIDTH)) -
                    3f * playerCollider.radius;
            if(Math.abs(newY) >= heightLimit) {
                newY = playerCollider.y;
            }
            if(Math.abs(newX) >= widthLimit) {
                newX = playerCollider.x;
            }
            if(newY < heightLimit) {
                playerCollider.setPosition(newX, newY);
            }
        }
        else if(Gdx.input.isKeyPressed(Input.Keys.S)) {
            float factorOfMovementOnYAxis = -(float)Math.sin(Math.toRadians(playerDirectionAngle));
            float factorOfMovementOnXAxis = -(float)Math.cos(Math.toRadians(playerDirectionAngle));

            // Normalize input
            float newY = factorOfMovementOnYAxis * playerMovementSpeed * deltaTime + playerCollider.y;

            float newX = factorOfMovementOnXAxis * playerMovementSpeed * deltaTime + playerCollider.x;
            float heightLimit = fitViewport.getWorldHeight() / 2f * (1 - 1 / (2f * GRID_HEIGHT)) -
                    3f * playerCollider.radius;
            float widthLimit = fitViewport.getWorldWidth() / 2f * (1 - 1 / (2f * GRID_WIDTH)) -
                    3f * playerCollider.radius;
            if(Math.abs(newY) >= heightLimit) {
                newY = playerCollider.y;
            }
            if(Math.abs(newX) >= widthLimit) {
                newX = playerCollider.x;
            }
            if(newY < heightLimit) {
                playerCollider.setPosition(newX, newY);
            }
        }
        if(Gdx.input.isKeyPressed(Input.Keys.D)) {
            playerDirectionAngle -= deltaTime * playerRotationSpeed;
            if(playerDirectionAngle <= 0) {
                playerDirectionAngle = 359.99f;
            }
        }
        else if(Gdx.input.isKeyPressed(Input.Keys.A)) {
            playerDirectionAngle += deltaTime * playerRotationSpeed;
            if(playerDirectionAngle >= 360) {
                playerDirectionAngle = 0;
            }
        }
    }

    private void drawGridSystem() {
        float widthDelta = fitViewport.getWorldWidth() / GRID_WIDTH;
        float heightDelta = fitViewport.getWorldHeight() / GRID_HEIGHT;

        // Draw vertical part of the grid
        for(int i = 1; i < GRID_WIDTH; i++) {
            game.drawer.line(-fitViewport.getWorldWidth() / 2 + widthDelta * i,
                    -fitViewport.getWorldHeight() / 2 + heightDelta,
                    -fitViewport.getWorldWidth() / 2 + widthDelta * i,
                    fitViewport.getWorldHeight() / 2 - heightDelta);
        }

        // Draw horizontal part of the grid
        for(int i = 1; i < GRID_HEIGHT; i++) {
            game.drawer.line(-fitViewport.getWorldWidth() / 2 + widthDelta,
                    -fitViewport.getWorldHeight() / 2 + heightDelta * i,
                    fitViewport.getWorldWidth() / 2 - widthDelta,
                    -fitViewport.getWorldHeight() / 2 + heightDelta * i);
        }
    }

    void drawPlayer() {
        game.drawer.filledCircle(playerCollider.x, playerCollider.y, playerCollider.radius);
        float slope = (float)Math.tan(Math.toRadians(playerDirectionAngle));
        game.drawer.setColor(Color.RED);
        float endLineX = playerCollider.x + (float)Math.cos(Math.toRadians(playerDirectionAngle)) * playerCollider.radius;
        float endLineY = playerCollider.y + (float)Math.sin(Math.toRadians(playerDirectionAngle)) * playerCollider.radius;
        game.drawer.line(playerCollider.x, playerCollider.y, endLineX, endLineY);


        game.drawer.setColor(Color.WHITE);
    }

    @Override
    public void resize(int width, int height) {
        fitViewport.update(width, height);
        screenViewport.update(width, height, true);

    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {

    }

    @Override
    public void dispose() {
        stage.dispose();
    }
}
