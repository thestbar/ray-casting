package com.thestbar.raycasting.util;

import com.badlogic.gdx.math.Rectangle;

public class CenteredRectangle extends Rectangle {
    public CenteredRectangle(float centeredX, float centeredY, float width, float height) {
        super(centeredX - width / 2, centeredY - height / 2, width, height);
    }
}
