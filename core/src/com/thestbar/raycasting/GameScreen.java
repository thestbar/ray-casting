package com.thestbar.raycasting;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import jdk.jfr.Timespan;

public class GameScreen implements Screen {
    private final RayCasting game;
    private OrthographicCamera camera;

    private Vector2 player = new Vector2(0, 0);
    private float playerMovementSpeed = 300;
    private Vector2 mapSize = new Vector2(32, 30);
    private Vector2 cellSize = new Vector2(32, 32);
    private int[] map = new int[(int)(mapSize.x * mapSize.y)];

    private Vector2 mouse;
    private boolean isDrawingLineBetweenPlayerAndMouse;


    public GameScreen(RayCasting game) {
        this.game = game;
//        camera = new OrthographicCamera();
        camera = new OrthographicCamera();
        // This fixes the problem were the input handling system counts
        // from top left when the draw is happening from bottom left
        // While rendering need to set projection matrix of sprite batch
        // to the matrix of the camera!
        camera.setToOrtho(true);

        // initialize player settings
        mouse = new Vector2();
        isDrawingLineBetweenPlayerAndMouse = false;
    }

    @Override
    public void show() {

    }

    private void input(float deltaTime) {
        mouse = new Vector2(Gdx.input.getX(), Gdx.input.getY());
        Vector2 cell = new Vector2((float)Math.floor(mouse.x / cellSize.x),
                (float)Math.floor(mouse.y / cellSize.y));

        // Paint with right mouse button "solid" tiles
        if(Gdx.input.isTouched()) map[(int)(cell.y * mapSize.x + cell.x)] = 1;

        // Move "player" position
        if(Gdx.input.isKeyPressed(Input.Keys.W)) player.y -= playerMovementSpeed * deltaTime;
        if(Gdx.input.isKeyPressed(Input.Keys.S)) player.y += playerMovementSpeed * deltaTime;
        if(Gdx.input.isKeyPressed(Input.Keys.A)) player.x -= playerMovementSpeed * deltaTime;
        if(Gdx.input.isKeyPressed(Input.Keys.D)) player.x += playerMovementSpeed * deltaTime;

        // If space is pressed then enable/disable line between player and mouse
        if(Gdx.input.isKeyJustPressed(Input.Keys.SPACE))
            isDrawingLineBetweenPlayerAndMouse = !isDrawingLineBetweenPlayerAndMouse;
    }

    @Override
    public void render(float delta) {

        ScreenUtils.clear(0, 0, 0, 1);

        game.batch.setProjectionMatrix(camera.combined);

        input(delta);

        // Draw Map
        drawMap2D();

        // Draw player
        drawPlayer2D();

        // Draw mouse
        drawMouse2D();

        // Cast ray
        castRaySlowAlgo();

        // Draw line between player and mouse
        drawLineBetweenPlayerAndMouse2D();
    }

    void castRaySlowAlgo() {
        Vector2 startPos = new Vector2(player);
        Vector2 endPos = new Vector2(mouse);
        final int RAY_STEPS = 100;
        // Find the normalized direction of the ray
        Vector2 dir = endPos.cpy().sub(startPos).nor();
        // Find angle from direction vector
        float angle = (float)(Math.atan2(dir.y, dir.x));
        // Get the slope of the line that connects the starting
        // and the ending position of the ray
        // Line for given x, then y = slope * (x - x0) + y0
        // where (x0, y0) can be start or end position
        float slope = (startPos.y - endPos.y) / (startPos.x - endPos.x);
        // Find the distance between start and end
        float distance = startPos.dst(endPos);
        // Break the distance to small pieces
        // which are going to be the steps
        float deltaDistance = distance / RAY_STEPS;
        // For this delta distance, calculate
        // delta movement on X axis
        float deltaX = (float)Math.cos(angle) * deltaDistance;
        // Perform the checks
        int i = 0;
        Vector2 intersection = new Vector2();
        boolean hitWall = false;
        while(i < RAY_STEPS) {
            // On each step find the current x position of the ray
            float currX = startPos.x + deltaX * i;
            // Using the line's coordinates constructor
            // find the current y position of the ray
            float currY = slope * (currX - startPos.x) + startPos.y;
            // Create a vector for the current position
            Vector2 point = new Vector2(currX, currY);
            // Using this vector find the grid position of the ray
            int gridX = (int)(point.x / cellSize.x);
            int gridY = (int)(point.y / cellSize.y);
            int value = map[(int)(gridY * mapSize.x + gridX)];
            // If value is 1 then ray hit a wall
            if(value == 1) {
                hitWall = true;
                intersection = point;
                break;
            }
            i++;
        }

        // Draw the ray if wall has been hit
        if(isDrawingLineBetweenPlayerAndMouse && hitWall) {
            game.batch.begin();
            game.drawer.setColor(Color.YELLOW);
            game.drawer.circle(intersection.x, intersection.y, cellSize.x / 4);
            game.batch.end();
        }
    }

    void drawMap2D() {
        for(int y = 0; y < mapSize.y; y++) {
            for(int x = 0; x < mapSize.x; x++) {
                int cell = map[(int) (y * mapSize.x + x)];
                game.batch.begin();
                // If cell has value 1 draw a color inside it
                if(cell == 1)
                    game.drawer.filledRectangle(x * cellSize.x, y * cellSize.y, cellSize.x, cellSize.y, Color.BLUE);
                // Draw cell boundary
                game.drawer.rectangle(x * cellSize.x, y * cellSize.y, cellSize.x, cellSize.y, Color.DARK_GRAY);
                game.batch.end();
            }
        }
    }

    void drawPlayer2D() {
        game.batch.begin();
        game.drawer.filledCircle(player, cellSize.x / 4, Color.RED);
        game.batch.end();
    }

    void drawMouse2D() {
        game.batch.begin();
        game.drawer.filledCircle(mouse, cellSize.x / 4, Color.GREEN);
        game.batch.end();
    }

    void drawLineBetweenPlayerAndMouse2D() {
        if(isDrawingLineBetweenPlayerAndMouse) {
            game.batch.begin();
            game.drawer.line(player, mouse, Color.WHITE);
            game.batch.end();
        }
    }

    @Override
    public void resize(int width, int height) {
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

    }
}