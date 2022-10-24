package com.thestbar.raycasting;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;

public class GameScreen implements Screen {
    private final RayCasting game;
    private OrthographicCamera camera;

    private Vector2 player = new Vector2(0, 0);
    private float playerMovementSpeed = 300;
    private Vector2 mapSize = new Vector2(32, 30);
    private Vector2 cellSize = new Vector2(32, 32);
    private int[] map = new int[(int)(mapSize.x * mapSize.y)];
    private Vector2 mouse;
    private Vector2 midRayEndPos;
    private boolean isDrawingLineBetweenPlayerAndMouse;
    private final int NUM_OF_RAYS = 100;
    private final int RAY_STEPS = 1000;
    private final float FOV = 80;
    private final float DRAW_DISTANCE = 400;


    public GameScreen(RayCasting game) {
        this.game = game;
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
        // Normalize mouse position, in order to have
        // fixed distance from player to the end of the rays
        Vector2 mouseDirNormalized = mouse.cpy().sub(player).nor();
        Vector2 mouseDir = mouseDirNormalized.cpy().scl(DRAW_DISTANCE);
        midRayEndPos = new Vector2(player.x + DRAW_DISTANCE * mouseDir.x,
                player.y + DRAW_DISTANCE * mouseDir.y);


        Vector2 cell = new Vector2((float)Math.floor(mouse.x / cellSize.x),
                (float)Math.floor(mouse.y / cellSize.y));

        // Paint with right mouse button "solid" tiles
        if(Gdx.input.isTouched()) map[(int)(cell.y * mapSize.x + cell.x)] = 1;

        // Move "player" position
        if(Gdx.input.isKeyPressed(Input.Keys.W)) {
            player.x += mouseDirNormalized.x * playerMovementSpeed * deltaTime;
            player.y += mouseDirNormalized.y * playerMovementSpeed * deltaTime;
        }
        else if(Gdx.input.isKeyPressed(Input.Keys.S)) {
            Vector2 behindFromMouseDirNorm = mouseDirNormalized.cpy().rotateDeg(180);
            player.x += behindFromMouseDirNorm.x * playerMovementSpeed * deltaTime;
            player.y += behindFromMouseDirNorm.y * playerMovementSpeed * deltaTime;
        }
        if(Gdx.input.isKeyPressed(Input.Keys.A)) {
            Vector2 leftFromMouseDirNorm = mouseDirNormalized.cpy().rotateDeg(-90);
            player.x += leftFromMouseDirNorm.x * playerMovementSpeed * deltaTime;
            player.y += leftFromMouseDirNorm.y * playerMovementSpeed * deltaTime;
        }
        else if(Gdx.input.isKeyPressed(Input.Keys.D)) {
            Vector2 rightFromMouseDirNorm = mouseDirNormalized.cpy().rotateDeg(90);
            player.x += rightFromMouseDirNorm.x * playerMovementSpeed * deltaTime;
            player.y += rightFromMouseDirNorm.y * playerMovementSpeed * deltaTime;
        }

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

        // Cast rays
        // When you know the origin point the length of the line and the direction
        // you can find the position of the point of the line by the formula below
        // (x, y) = (x1 + a * l, y1 + b * l)
        // Where l is the length of the line
        // x1, y1 are the coordinates of the starting point
        // a, b are the coordinates of the direction vector

        Vector2 currRayDir = midRayEndPos.cpy().sub(player).nor().rotateDeg(-FOV / 2);
        final float rayStep = FOV / NUM_OF_RAYS;
        for(int i = 0; i < NUM_OF_RAYS; i++) {
            Vector2 rayEndPoint = new Vector2(player.x + DRAW_DISTANCE * currRayDir.x,
                    player.y + DRAW_DISTANCE * currRayDir.y);
            castRaySlowAlgo(player, rayEndPoint);
            currRayDir.rotateDeg(rayStep);
        }

        // Draw line between player and mouse
        drawLineBetweenPlayerAndMouse2D();
    }

    void castRaySlowAlgo(Vector2 sPos, Vector2 ePos) {
        Vector2 startPos = new Vector2(sPos);
        Vector2 endPos = new Vector2(ePos);

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

            // If it goes out of screen for X or Y axis stop going further
            if(gridX < 0) gridX = 0; if(gridX > mapSize.x - 1) gridX = (int)mapSize.x - 1;
            if(gridY < 0) gridY = 0; if(gridY > mapSize.y - 1) gridY = (int)mapSize.y - 1;

            int cellPos = (int)(gridY * mapSize.x + gridX);
            if(cellPos >= 0 && cellPos <= mapSize.x * mapSize.y - 1) {
                int value = map[cellPos];
                // If value is 1 then ray hit a wall
                if(value == 1) {
                    hitWall = true;
                    intersection = point;
                    break;
                }
                else {
                    intersection = point;
                }
                i++;
            }
        }
        // Draw ray
        game.batch.begin();
        game.drawer.setColor(Color.WHITE);
        game.drawer.line(startPos, intersection);
        game.batch.end();

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
            game.drawer.line(player, mouse, Color.GREEN);
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
