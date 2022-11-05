package com.thestbar.raycasting;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import com.thestbar.raycasting.util.CenteredRectangle;

import java.io.IOException;

public class GameScreen implements Screen {
    private final RayCasting game;
    private OrthographicCamera camera;

    private Vector2 player = new Vector2(300, 40);
    private Vector2 playerDir = new Vector2(1, 0);
    private float playerMovementSpeed = 100;
    private float playerRotationMovementSpeed = 150;
    private Vector2 mapSize = new Vector2(16, 15);
    private Vector2 cellSize = new Vector2(40, 40);
    // TODO - Remove dependency from initializing array (handle DDA ray caster not to error if ray out of bounds
    private int[] map = new int[(int)(mapSize.x * mapSize.y)];
    private Vector2 mouse;
    private Vector2 midRayEndPos;
    private boolean isDrawingLineBetweenPlayerAndMouse;
    private final int NUM_OF_RAYS = 1000;
    private final int RAY_STEPS = 100;
    private final float FOV = 50;
    private final float DRAW_DISTANCE = 400;
    // MAX_DISTANCE is only used with Slow ray caster
    // TODO Add max draw distance functionality in DDA ray caster?
    private final float MAX_DISTANCE = 800;
    private float fpsCounterInterval = 0;
    private final float UPDATE_FPS_INTERVAL = 1;
    private float[] rayDistances = new float[NUM_OF_RAYS];
    private int[] rayValues = new int[NUM_OF_RAYS];
    private boolean[] rayHitSideValue = new boolean[NUM_OF_RAYS];
    // Contains the Path to current level
    private final String LEVEL_MAP_PATH = "./levelMaps/Level1_Map.txt";

    public GameScreen(RayCasting game) throws IOException {
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

        // Initialize map
        initializeMap();
    }

    @Override
    public void show() {

    }

    private void input(float deltaTime) {
        mouse = new Vector2(Gdx.input.getX(), Gdx.input.getY());
        // Normalize mouse position, in order to have
        // fixed distance from player to the end of the rays
        midRayEndPos = new Vector2(player.x + DRAW_DISTANCE * playerDir.x,
                player.y + DRAW_DISTANCE * playerDir.y);

        Vector2 cell = new Vector2((float)Math.floor(mouse.x / cellSize.x),
                (float)Math.floor(mouse.y / cellSize.y));

        // Paint with right mouse button "solid" tiles
        if(Gdx.input.isTouched()) map[(int)(cell.y * mapSize.x + cell.x)] = 1;

        // Get a copy of player's current position
        Vector2 playerCpy = player.cpy();

        // Move "player" position
        if(Gdx.input.isKeyPressed(Input.Keys.W)) {
            playerCpy.x += playerDir.x * playerMovementSpeed * deltaTime;
            playerCpy.y += playerDir.y * playerMovementSpeed * deltaTime;
        }
        else if(Gdx.input.isKeyPressed(Input.Keys.S)) {
            Vector2 behindFromPlayerDir = playerDir.cpy().rotateDeg(180);
            playerCpy.x += behindFromPlayerDir.x * playerMovementSpeed * deltaTime;
            playerCpy.y += behindFromPlayerDir.y * playerMovementSpeed * deltaTime;
        }
        if(Gdx.input.isKeyPressed(Input.Keys.A)) {
            playerDir.rotateDeg(-deltaTime * playerRotationMovementSpeed);
        }
        else if(Gdx.input.isKeyPressed(Input.Keys.D)) {
            playerDir.rotateDeg(deltaTime * playerRotationMovementSpeed);
        }
        if(Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            Vector2 leftFromPlayerDir = playerDir.cpy().rotateDeg(-90);
            playerCpy.x += leftFromPlayerDir.x * playerMovementSpeed * deltaTime;
            playerCpy.y += leftFromPlayerDir.y * playerMovementSpeed * deltaTime;
        }
        else if(Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            Vector2 rightFromPlayerDir = playerDir.cpy().rotateDeg(90);
            playerCpy.x += rightFromPlayerDir.x * playerMovementSpeed * deltaTime;
            playerCpy.y += rightFromPlayerDir.y * playerMovementSpeed * deltaTime;
        }

        detectCollisions(playerCpy);

        // If space is pressed then enable/disable line between player and mouse
        if(Gdx.input.isKeyJustPressed(Input.Keys.SPACE))
            isDrawingLineBetweenPlayerAndMouse = !isDrawingLineBetweenPlayerAndMouse;
    }

    @Override
    public void render(float delta) {
        // Set FPS counter in Desktop title
        countFps(delta);

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
            // Selection between 2 different ray casters
            // castRaySlowAlgo(player, rayEndPoint, i, Math.abs(currRayDir.angleDeg() - playerDir.angleDeg()));
            castRayDDAAlgo(player, rayEndPoint, i, Math.abs(currRayDir.angleDeg() - playerDir.angleDeg()));
            currRayDir.rotateDeg(rayStep);
        }

        // Draw 3D screen
        drawScreen3D();

        // Draw line between player and mouse
        drawLineBetweenPlayerAndMouse2D();
    }

    void countFps(float delta) {
        int fps = (int)(1 / delta);
        if(fpsCounterInterval >= UPDATE_FPS_INTERVAL) {
            Gdx.graphics.setTitle("RayCasting - FPS: " + fps);
            fpsCounterInterval = 0;
        }
        else {
            fpsCounterInterval += delta;
        }
    }

    void drawScreen3D() {
        for(int i = 0; i < NUM_OF_RAYS; i++) {
            // Get the current ray's distance (it has the perpendicular distance
            float currRayDist = rayDistances[i];

            // Calculate the number of pixels that each column of the 3D contains
            float pixelsOfEachCol = Gdx.graphics.getWidth() / 2f / NUM_OF_RAYS;

            // If the ray hit the Y-Axis then make the wall a bit darker
            // This makes the 3D screen looking better, visually
            float alpha = (rayHitSideValue[i]) ? 0.7f : 1;

            // Calculate the height of the column of the 3D screen
            // This is calculated by dividing the maximum height of the screen
            // by the perpendicular distance of the intersection from the camera pane
            // Also, we multiply this by a final variable, to make the walls higher
            float rectangleHeight = 70 * Gdx.graphics.getHeight() / currRayDist;

            // Offset on the X-Axis of the screen is calculated
            // (This applies only when both 2D and 3D worlds are drawn
            float xOffset = Gdx.graphics.getWidth() / 2f;

            // Create the centered rectangle of the wall
            Rectangle rectangle = new CenteredRectangle(xOffset + i * pixelsOfEachCol + pixelsOfEachCol / 2,
                    Gdx.graphics.getHeight() / 2f, pixelsOfEachCol, rectangleHeight);

            // Create the ceiling rectangle (above the wall)
            Rectangle ceilingRectangle = new Rectangle(xOffset + i * pixelsOfEachCol,
                    0, pixelsOfEachCol, (Gdx.graphics.getHeight() - rectangleHeight) / 2f);

            // Create the floor rectangle (below the wall)
            Rectangle floorRectangle = new Rectangle(xOffset + i * pixelsOfEachCol,
                    rectangle.y + rectangle.height, pixelsOfEachCol, (Gdx.graphics.getHeight() - rectangleHeight) / 2f);

            // Select the screen color based on the map value
            Color color;
            switch(rayValues[i]) {
                case 1: { color = new Color(Color.BLUE); break; }
                case 2: { color = new Color(Color.RED); break; }
                case 3: { color = new Color(Color.YELLOW); break; }
                case 4: { color = new Color(Color.GREEN); break; }
                default: { color = new Color(Color.WHITE); }
            }

            // Apply the appropriate alpha value
            color.a = alpha;

            // Draw the rectangles in the 3D screen
            game.batch.begin();
            game.drawer.setColor(color);
            game.drawer.filledRectangle(rectangle);
            game.drawer.setColor(Color.DARK_GRAY);
            game.drawer.filledRectangle(floorRectangle);
            game.drawer.setColor(Color.LIGHT_GRAY);
            game.drawer.filledRectangle(ceilingRectangle);
            game.batch.end();
        }
    }

    void castRayDDAAlgo(Vector2 sPos, Vector2 ePos, int rayIndex, float angleBetweenCameraAndRay) {
        Vector2 startPos = sPos.cpy();
        Vector2 endPos = ePos.cpy();

        // Find the normalized direction of the ray
        Vector2 rayDir = endPos.cpy().sub(startPos).nor();

        // Find angle from direction vector
        float angle = (float)(Math.atan2(rayDir.y, rayDir.x));

        // Find the tile on which the player is
        // Using this vector find the grid position of the ray
        int posX = (int)(startPos.x / cellSize.x);
        int posY = (int)(startPos.y / cellSize.y);

        // If it goes out of screen for X or Y axis stop going further
        if(posX < 0) posX = 0; if(posX > mapSize.x - 1) posX = (int)mapSize.x - 1;
        if(posY < 0) posY = 0; if(posY > mapSize.y - 1) posY = (int)mapSize.y - 1;

        // Create variables that will hold the value of the length from player till
        // the current position of the ray for both travelling on X and Y axis
        float currLenDeltaX;
        float currLenDeltaY;

        // Calculate the step on the length when moving on X and Y axis
        float stepDeltaX = (float)Math.abs(cellSize.x / Math.cos(angle));
        float stepDeltaY = (float)Math.abs(cellSize.y / Math.sin(angle));

        // Calculate the initial values (before starting the actual DDA)
        // If ray's direction vector x value is > 0 then this means
        // that when travelling on X axis the ray will go to the right
        // If it is < 0 then it will go to the left
        if(rayDir.x > 0) {
            float rightBarrier = cellSize.x * (posX + 1);
            float deltaX = rightBarrier - startPos.x;
            currLenDeltaX = deltaX / (float)Math.cos(angle);
        }
        else {
            float leftBarrier = cellSize.x * posX;
            float deltaX = leftBarrier - startPos.x;
            currLenDeltaX = deltaX / (float)Math.cos(angle);
        }

        // Similar way when rayDir.y > 0 then the ray will move down
        // and when rayDir.y < 0 then the ray will move up
        if(rayDir.y > 0) {
            float downBarrier = cellSize.y * (posY + 1);
            float deltaY = downBarrier - startPos.y;
            currLenDeltaY = deltaY / (float)Math.sin(angle);
        }
        else {
            float topBarrier = cellSize.y * posY;
            float deltaY = topBarrier - startPos.y;
            currLenDeltaY = deltaY / (float)Math.sin(angle);
        }

        // Boolean variable that indicates that the ray hit a wall
        boolean hit = false;

        // Boolean variables that indicates if the hit was made
        // on an X or Y axis wall
        boolean movedOnXAxis;

        // Vector that will hold the intersection between wall and ray
        Vector2 rayEndPos = new Vector2();

        // Actual DDA starts here
        while(!hit) {
            // Always select to move on the direction which has the smallest length
            if(currLenDeltaX < currLenDeltaY) {
                // Move delta X
                currLenDeltaX += stepDeltaX;
                if(rayDir.x > 0) posX++;
                else posX--;
                movedOnXAxis = true;
            }
            else {
                // Move delta Y
                currLenDeltaY += stepDeltaY;
                if(rayDir.y > 0) posY++;
                else posY--;
                movedOnXAxis = false;
            }

            // Check for collisions with walls
            int currCellPos = (int)(posY * mapSize.x + posX);
            if(map[currCellPos] > 0) {
                hit = true;
                if(movedOnXAxis) {
                    // Calculate intersection based on currLenDeltaX
                    rayEndPos = startPos.cpy().add(rayDir.cpy().scl(currLenDeltaX - stepDeltaX));
                    rayDistances[rayIndex] = (currLenDeltaX - stepDeltaX) *
                            (float)Math.cos(Math.toRadians(angleBetweenCameraAndRay));
                    rayHitSideValue[rayIndex] = true;
                }
                else {
                    // Calculate intersection based on currLenDeltaY
                    rayEndPos = startPos.cpy().add(rayDir.cpy().scl(currLenDeltaY - stepDeltaY));
                    rayDistances[rayIndex] = (currLenDeltaY - stepDeltaY) *
                            (float)Math.cos(Math.toRadians(angleBetweenCameraAndRay));
                    rayHitSideValue[rayIndex] = false;
                }

                // Save the type of the wall that was hit
                rayValues[rayIndex] = map[(int)(posY * mapSize.x + posX)];
            }
        }

        // Currently this check is useless because always it hit a wall
        if(hit) {
            // Draw line
            game.batch.begin();
            game.drawer.setColor(Color.WHITE);
            game.drawer.line(startPos, rayEndPos);
            game.batch.end();
        }

        // Draw yellow circles if space bar is clicked
        if(isDrawingLineBetweenPlayerAndMouse && hit) {
            game.batch.begin();
            game.drawer.setColor(Color.YELLOW);
            game.drawer.circle(rayEndPos.x, rayEndPos.y, cellSize.x / 4);
            game.batch.end();
        }
    }

    void castRaySlowAlgo(Vector2 sPos, Vector2 ePos, int index, float angleBetweenCameraAndRay) {
        // Index represents the index of
        // the current ray that is calculated

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
                if(value > 0) {
                    hitWall = true;
                    intersection = point;
                    // Save the map value of the current intersection
                    rayValues[index] = map[cellPos];
                    break;
                }
                else {
                    intersection = point;
                }
                i++;
            }
        }

        // Store the distance between the starting position
        // and the intersection on the specific index in the array
        // In order to fix fish-eye-effect instead of using
        // the euclidean distance we have to multiply it
        // by the cosine of the angle between the current ray
        // and the direction of the camera
        rayDistances[index] = intersection.dst(startPos) *
                (float)Math.cos(Math.toRadians(angleBetweenCameraAndRay));

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
                else if(cell == 2)
                    game.drawer.filledRectangle(x * cellSize.x, y * cellSize.y, cellSize.x, cellSize.y, Color.RED);
                else if(cell == 3)
                    game.drawer.filledRectangle(x * cellSize.x, y * cellSize.y, cellSize.x, cellSize.y, Color.YELLOW);
                else if(cell == 4)
                    game.drawer.filledRectangle(x * cellSize.x, y * cellSize.y, cellSize.x, cellSize.y, Color.GREEN);
                // Draw cell boundary
                game.drawer.rectangle(x * cellSize.x, y * cellSize.y, cellSize.x, cellSize.y, Color.DARK_GRAY);
                game.batch.end();
            }
        }
    }

    void detectCollisions(Vector2 playerPosition) {
        // Check if new position is valid
        // Find the tile that the new position is into
        // if it is a wall do not let it move
        Vector2 playerNewPositionCell = new Vector2((float)Math.floor(playerPosition.x / cellSize.x),
                (float)Math.floor(playerPosition.y / cellSize.y));
        if(map[(int)(playerNewPositionCell.y * mapSize.x + playerNewPositionCell.x)] == 0) {
            player = playerPosition;
        }
        // If the new position is invalid
        // Check if only moving left/right is valid
        else {
            Vector2 playerPositionCpy = player.cpy();
            playerPositionCpy.x = playerPosition.x;
            // Check if position with only x added is valid
            playerNewPositionCell = new Vector2((float)Math.floor(playerPositionCpy.x / cellSize.x),
                    (float)Math.floor(playerPositionCpy.y / cellSize.y));
            if(map[(int)(playerNewPositionCell.y * mapSize.x + playerNewPositionCell.x)] == 0) {
                player = playerPositionCpy;
            }
            // If new position is invalid and
            // moving left or right is invalid
            // Check if only moving up/down is valid
            else {
                playerPositionCpy = player.cpy();
                playerPositionCpy.y = playerPosition.y;
                // Check if position with only y added is valid
                playerNewPositionCell = new Vector2((float)Math.floor(playerPositionCpy.x / cellSize.x),
                        (float)Math.floor(playerPositionCpy.y / cellSize.y));
                if(map[(int)(playerNewPositionCell.y * mapSize.x + playerNewPositionCell.x)] == 0) {
                    player = playerPositionCpy;
                }
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

    void initializeMap() throws IOException {
        GridSetup gridSetup = new GridSetup(LEVEL_MAP_PATH, (int)mapSize.y, (int)mapSize.x);
        map = gridSetup.getGrid();
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
