package io.github.wysohn.realeconomy.manager.business.types.mining;

import io.github.wysohn.rapidframework3.interfaces.IMemento;
import io.github.wysohn.realeconomy.manager.business.types.AbstractBusiness;

import java.util.UUID;

public class MiningBusiness extends AbstractBusiness {
    private MiningBusiness() {
        super(null);
    }

    public MiningBusiness(UUID key) {
        super(key);
    }

    @Override
    public void update() {
        super.update();


    }

    @Override
    public IMemento saveState() {
        return null;
    }

    @Override
    public void restoreState(IMemento iMemento) {

    }
}
