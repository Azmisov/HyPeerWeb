/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package hypeerweb;

/**
 *
 * @author Brian
 */
 public class FoldState{
    private boolean isStable;
    
    public FoldState(boolean isStable){
        this.isStable = isStable;
    }
    
    public void updateFolds(Node.FoldDatabaseChanges fdc, Node caller, Node child){
        if (caller.getInverseSurrogateFold() != null)
            updateUnstableState(fdc, caller, child);
        else updateStableState(fdc, caller, child);
    }
    
    private void updateUnstableState(Node.FoldDatabaseChanges fdc, Node caller, Node child){
        //Update reflexive folds
        fdc.updateDirect(child, caller.getFold());
        fdc.updateDirect(caller.getFold(), child);
        //Insert surrogates for non-existant node
        fdc.updateSurrogate(caller, caller.getFold());
        fdc.updateInverse(caller.getFold(), caller);
        //Remove stable state reference
        fdc.removeDirect(caller, null);
    }
    private void updateStableState(Node.FoldDatabaseChanges fdc, Node caller, Node child){
        //Stable-state fold references
        fdc.updateDirect(child, caller.getInverseSurrogateFold());
        fdc.updateDirect(caller.getInverseSurrogateFold(), child);
        //Remove surrogate references
        fdc.removeSurrogate(caller.getInverseSurrogateFold(), null);
        fdc.removeInverse(caller, null);
    }
}
