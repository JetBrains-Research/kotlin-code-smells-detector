package org.jetbrains.research.kotlincodesmelldetector.core.distance;

import org.jetbrains.kotlin.psi.KtClassOrObject;
import org.jetbrains.kotlin.psi.KtElement;
import org.jetbrains.kotlin.psi.KtProperty;

import java.util.Set;

public class PropertyEntity extends Entity {
    public PropertyEntity(KtProperty element) {
        super(element);
    }

    @Override
    public Set<Entity> getEntitySet() {
        //TODO
        return null;
    }

    @Override
    public Set<Entity> getFullEntitySet() {
        //TODO
        return null;
    }

    @Override
    public KtClassOrObject getKtClass() {
        //TODO
        return null;
    }
}
