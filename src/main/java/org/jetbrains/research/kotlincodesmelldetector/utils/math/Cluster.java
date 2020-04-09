package org.jetbrains.research.kotlincodesmelldetector.utils.math;

import com.intellij.psi.SmartPsiElementPointer;
import org.jetbrains.kotlin.psi.KtDeclaration;

import java.util.ArrayList;

public class Cluster {

    private final ArrayList<SmartPsiElementPointer<? extends KtDeclaration>> entities;
    private int hashCode;

    public Cluster() {
        entities = new ArrayList<>();
    }

    public Cluster(ArrayList<SmartPsiElementPointer<? extends KtDeclaration>> entities) {
        this.entities = new ArrayList<>(entities);
    }

    public void addEntity(SmartPsiElementPointer<? extends KtDeclaration> entity) {
        if (!entities.contains(entity)) {
            entities.add(entity);
        }
    }

    public ArrayList<SmartPsiElementPointer<? extends KtDeclaration>> getEntities() {
        return entities;
    }

    public void addEntities(ArrayList<SmartPsiElementPointer<? extends KtDeclaration>> entities) {
        if (!this.entities.containsAll(entities)) {
            this.entities.addAll(entities);
        }
    }

    public int hashCode() {
        if (hashCode == 0) {
            int result = 17;
            for (SmartPsiElementPointer<? extends KtDeclaration> entity : entities) {
                KtDeclaration element = entity.getElement();
                if (element != null) {
                    result = 37 * result + entity.getElement().hashCode();
                }
            }

            hashCode = result;
        }
        return hashCode;
    }

    public String toString() {
        StringBuilder s = new StringBuilder("{");

        for (SmartPsiElementPointer<? extends KtDeclaration> entity : entities) {
            s.append(entity).append(", ");
        }
        s.append("}");
        return s.toString();
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Cluster)) {
            return false;
        }

        return this.entities.equals(((Cluster) other).entities);
    }
}
