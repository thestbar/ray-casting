package com.thestbar.raycasting;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

public class GameScreen implements Screen {
    private final RayCasting game;
    private OrthographicCamera camera;
    // Viewport used for game world
    private float playerX;
    private float playerY;
    private float playerDeltaX;
    private float playerDeltaY;
    private float playerAngle;
    private float playerRadius;
    private int mapX = 8;
    private int mapY = 8;
    private int mapS = 64;
    private int[] map = {
            1,1,1,1,1,1,1,1,
            1,0,0,1,0,0,0,1,
            1,0,0,1,0,0,0,1,
            1,0,0,1,0,0,0,1,
            1,0,0,0,0,0,0,1,
            1,0,0,0,0,1,0,1,
            1,0,0,0,0,0,0,1,
            1,1,1,1,1,1,1,1
    };

    public GameScreen(RayCasting game) {
        this.game = game;
        camera = new OrthographicCamera();

        // initialize player settings
        playerX = 300;
        playerY = 300;
        playerRadius = 8;
        playerAngle = 0;
        playerDeltaX = (float)Math.cos(playerAngle) * 5;
        playerDeltaY = (float)Math.sin(playerAngle) * 5;
    }

    @Override
    public void show() {

    }

    private void input() {
        if(Gdx.input.isKeyPressed(Input.Keys.A)) {
            playerAngle -= 0.1;
            if(playerAngle < 0) playerAngle += 2 * (float)Math.PI;
            playerDeltaX = (float)Math.cos(playerAngle) * 5;
            playerDeltaY = (float)Math.sin(playerAngle) * 5;
        }
        if(Gdx.input.isKeyPressed(Input.Keys.D)) {
            playerAngle += 0.1;
            if(playerAngle > 2 * (float)Math.PI) playerAngle -= 2 * (float)Math.PI;
            playerDeltaX = (float)Math.cos(playerAngle) * 5;
            playerDeltaY = (float)Math.sin(playerAngle) * 5;
        }
        if(Gdx.input.isKeyPressed(Input.Keys.W)) {
            playerX += playerDeltaX;
            playerY += playerDeltaY;
        }
        if(Gdx.input.isKeyPressed(Input.Keys.S)) {
            playerX -= playerDeltaX;
            playerY -= playerDeltaY;
        }
    }

    private void drawPlayer() {
        game.batch.begin();
        game.drawer.setColor(Color.YELLOW);
        game.drawer.filledCircle(playerX, playerY, playerRadius);
        game.batch.end();

        // draw direction line
        game.batch.begin();
        game.drawer.setColor(Color.YELLOW);
        game.drawer.line(playerX,
                playerY,
                playerX + playerDeltaX * 5,
                playerY + playerDeltaY * 5, 3);
        game.batch.end();
    }

    private void drawMap2D() {
        int x, y, xo, yo;
        for(y = mapY - 1; y >= 0; y--) {
            for(x = mapX - 1; x >= 0; x--) {
                // if this is a wall then draw it as white
                // else draw the tile as black
                if(map[y * mapX + x] == 1) {
                    game.drawer.setColor(Color.WHITE);
                }
                else {
                    game.drawer.setColor(Color.BLACK);
                }
                xo = x * mapS;
                yo = y * mapS;
                game.batch.begin();
                Rectangle rectangle = new Rectangle( x + xo, 512 - y - yo - mapS, mapS, mapS);
                game.drawer.filledRectangle(rectangle);
                game.batch.end();
            }
        }
    }

    private void drawRays3D() {
        int ray, mapPositionX, mapPositionY, mapPosition, depthOfField;
        float rayX = 0, rayY = 0, rayAngle = playerAngle, xOffset = 0, yOffset = 0;
        for(ray = 0; ray < 1; ray++) {
            // Check Horizontal Lines - Rays
            depthOfField = 0;
            float angleTan = -1 / (float)Math.tan(rayAngle);
            // need to now if the ray is looking up or down
            // we know that by checking the angle of the ray
            // if ray angle > PI then it looks down
            if(rayAngle > Math.PI) { // looking down
                // we need to round the y position of the ray to
                // the nearest 64's value
                // 1) divide the value by 64 (bit shifting 6-down)
                // 2) multiply by 64 (bit shifting 6-up)
                // 3) subtract a small number for accuracy
                rayY = (((int)playerY >> 6) << 6) - 0.0001f;
                // ray's x position is the distance between the
                // player's and ray's y position multiplied by
                // the inverse tangent of the angle of the ray
                // plus the x position of the player
                rayX = (playerY - rayY) * angleTan + playerX;
                // when we find the ray's 1st hit position then
                // we need to find the next x and y offset
                // yOffset is found by subtracting 64 units
                // xOffset is xOffset multiplied by inverse
                // tangent of the angle of the ray
                yOffset = -64;
                xOffset = -yOffset * angleTan;
            }
            if(rayAngle < Math.PI) { // looking up
                // ray y position is the same but instead of
                // subtracting a small number we add value 64
                rayY = (((int)playerY >> 6) << 6) + 64;
                // everything else is the same except for that
                // the y offset is positive instead of negative
                rayX = (playerY - rayY) * angleTan + playerX;
                yOffset = 64;
                xOffset = -yOffset * angleTan;
            }
            if(rayAngle == 0 || rayAngle == Math.PI) { // looking straight left or right
                // if it looks horizontally this means that
                // it will never hit a wall, so we have to
                // add a maximum field of view to stop checking
                rayX = playerX;
                rayY = playerY;
                depthOfField = 8;
            }
            while(depthOfField < 8) {
                // we know the large coordinates where the ray will hit
                // the wall, but we need to know where that is in the map array
                // 1) take ray's x position and divide it by 64
                // 2) set that to find the position in the maps array
                mapPositionX = (int) rayX >> 6;
                mapPositionY = (int) rayY >> 6;
                mapPosition = mapPositionY * mapX + mapPositionX;
                // if map position is less than the array size then we can check it
                // inside the map and if the value is 1 then there is a wall there
                if(mapPosition < mapX * mapY && map[mapPosition] == 1) { // hit wall
                    depthOfField = 8;
                }
                else { // did not hit wall
                    // check next horizontal line by adding x and y offset
                    rayX += xOffset;
                    rayY += yOffset;
                    depthOfField++; // go to next line
                }
                // draw ray to screen
                game.batch.begin();
                game.drawer.setColor(Color.GREEN);
                game.drawer.line(playerX, playerY, rayX, rayY);
                game.batch.end();
            }
        }
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0.3f, 0.3f, 0.3f, 1);
        // Apply viewport for game world
//        fitViewport.apply();

        // Tell camera to update its matrices
        camera.update();
//        game.batch.setProjectionMatrix(camera.combined);

        // handle player input
        input();

        // before drawing the player
        // draw the 2D grid
        drawMap2D();

        // draw player
        drawPlayer();

        // draw rays
        drawRays3D();
//        System.out.println(Math.toDegrees(playerAngle));
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
