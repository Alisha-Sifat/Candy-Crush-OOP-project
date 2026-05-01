package com.candycrush;

import java.awt.Image;

public abstract class Candy {
    protected int type;
    protected Image image;

    public Candy(int type, Image image) {
        this.type = type;
        this.image = image;
    }

    public Image getImage() {
        return image;
    }

    public int getType() {
        return type;
    }

    public abstract void onMatch();
}