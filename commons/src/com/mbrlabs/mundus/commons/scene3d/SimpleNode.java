/*
 * Copyright (c) 2016. See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mbrlabs.mundus.commons.scene3d;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

/**
 * @author Marcus Brummer
 * @version 09-06-2016
 */
public class SimpleNode<T extends SimpleNode> implements Node<T> {

    private Vector3         localPosition;
    private Quaternion      localRotation;
    private Vector3         localScale;

    // root * p0 * p1 * localMat = combined (absolute transfrom)
    private Matrix4         combined;

    private Array<T> children;
    private T parent;

    public SimpleNode() {
        localPosition = new Vector3();
        localRotation = new Quaternion();
        localScale = new Vector3(1, 1, 1);
        combined = new Matrix4();
    }

    @Override
    public Vector3 getLocalPosition(Vector3 out) {
        return out.set(localPosition);
    }

    @Override
    public Quaternion getLocalRotation(Quaternion out) {
        return out.set(localRotation);
    }

    @Override
    public Vector3 getLocalScale(Vector3 out) {
        return out.set(localScale);
    }

    @Override
    public Vector3 getPosition(Vector3 out) {
        return toMatrix().getTranslation(out);
    }

    @Override
    public Quaternion getRotation(Quaternion out) {
        return toMatrix().getRotation(out);
    }

    @Override
    public Vector3 getScale(Vector3 out) {
        return toMatrix().getScale(out);
    }

    @Override
    public Matrix4 toMatrix() {
        if(parent == null) return combined.set(localPosition, localRotation, localScale);
        combined.set(localPosition, localRotation, localScale);
        return combined.mulLeft(parent.toMatrix());
    }

    @Override
    public void translate(Vector3 v) {
        localPosition.add(v);
    }

    @Override
    public void translate(float x, float y, float z) {
        localPosition.add(x, y, z);
    }

    @Override
    public void rotate(Quaternion q) {
        localRotation.mulLeft(q);
    }

    @Override
    public void rotate(float x, float y, float z, float w) {
        localRotation.mulLeft(x, y, z, w);
    }

    @Override
    public void scale(Vector3 v) {
        localScale.scl(v);
    }

    @Override
    public void scale(float x, float y, float z) {
        localScale.scl(x, y, z);
    }

    @Override
    public void setLocalPosition(float x, float y, float z) {
        localPosition.set(x, y, z);
    }

    @Override
    public void addChild(T child) {
        if(children == null) children = new Array<T>();
        children.add(child);
        child.setParent(this);
    }

    @Override
    public Array<T> getChildren() {
        return this.children;
    }

    @Override
    public T getParent() {
        return this.parent;
    }

    @Override
    public void setParent(T parent) {
        this.parent = parent;
    }

    @Override
    public void remove() {
        // TODO implement
    }

}
