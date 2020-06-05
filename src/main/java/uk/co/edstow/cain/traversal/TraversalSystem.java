package uk.co.edstow.cain.traversal;

import uk.co.edstow.cain.structures.WorkState;

public  interface TraversalSystem {

    void add(WorkState child, WorkState next);
    void add(WorkState child);
    WorkState poll();
    WorkState steal(TraversalSystem system) throws InterruptedException;

}
