package won.bot.icb.utils;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import won.protocol.model.Coordinate;
import won.protocol.util.DefaultAtomModelWrapper;

import java.net.URI;

public class ICBAtomModelWrapper extends DefaultAtomModelWrapper {

    public ICBAtomModelWrapper(URI atomUri) {
        this(atomUri.toString());
    }

    public ICBAtomModelWrapper(String atomUri) {
        super(atomUri);
    }

    public ICBAtomModelWrapper(Dataset atomDataset) {
        super(atomDataset);
    }

    public Coordinate getSeeksLocationCoordinate(){
        Resource seeks = null;
        for (Resource r : getSeeksNodes()) {
            seeks = r;
            break;
        }
        return getLocationCoordinate(seeks);
    }

}
