package org.jetbrains.research.kotlincodesmelldetector.core.distance;

import com.intellij.psi.SmartPsiElementPointer;
import org.jetbrains.kotlin.psi.KtClassOrObject;
import org.jetbrains.kotlin.psi.KtElement;
import org.jetbrains.research.kotlincodesmelldetector.utils.PsiUtils;

import java.util.Objects;
import java.util.Set;

public abstract class Entity {
    private SmartPsiElementPointer<KtElement> elementPointer;

    public Entity(KtElement element) {
        elementPointer = PsiUtils.toPointer(element);
    }

    public abstract Set<Entity> getEntitySet();

    public abstract Set<Entity> getFullEntitySet();

    public abstract KtClassOrObject getKtClass();

    @Override
    public boolean equals(Object entity) {
        return entity instanceof Entity && Objects.equals(elementPointer.getElement(), ((Entity) entity).elementPointer.getElement());
    }
}
