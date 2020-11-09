package vanguard;

public final class VLTaskIncreasingThreshold<VARTYPE extends VLV> extends VLTask<VARTYPE>{

    private float threshold;
    private boolean ran = false;


    public VLTaskIncreasingThreshold(Task<VARTYPE> task, float threshold){
        super(task);
        this.threshold = threshold;
    }


    @Override
    protected boolean checkRun(VARTYPE v){
        if(!ran){
            float val = v.get();
            boolean increasing = v.getIncreasing();

            if(increasing && val >= threshold){
                task.run(this, v);
                ran = true;

                return true;
            }
        }

        return false;
    }

    @Override
    protected void reset(){
        ran = false;
    }
}
