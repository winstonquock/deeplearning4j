package org.deeplearning4j.iterativereduce.impl.multilayer;



import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collection;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.util.ToolRunner;
import org.deeplearning4j.scaleout.api.ir.ParameterVectorUpdateable;
import org.deeplearning4j.iterativereduce.runtime.ComputableMaster;
import org.deeplearning4j.iterativereduce.runtime.yarn.appmaster.ApplicationMaster;
import org.nd4j.linalg.factory.Nd4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Master node:
 *
 *
 */
public class Master implements ComputableMaster<ParameterVectorUpdateable> {

    ParameterVectorUpdateable lastMasterUpdate = null;
    protected Configuration conf = null;
    protected static Logger log = LoggerFactory.getLogger(Master.class);

    /**
     * Q: "is compute() called before complete() is called in last epoch?"
     *
     *
     */
    @Override
    public void complete(DataOutputStream osStream) throws IOException {
        log.info( "IR DBN Master Node: Complete!" );
        Nd4j.write(lastMasterUpdate.get(),osStream);
    }


    /**
     * Master::Compute
     *
     * This is where the worker parameter averaged updates come in and are processed
     *
     */
    @Override
    public ParameterVectorUpdateable compute(
            Collection<ParameterVectorUpdateable> workerUpdates,
            Collection<ParameterVectorUpdateable> masterUpdates) {

        log.info( "--------------- Master::Compute() -------------- " );
        ParameterVectorUpdateable first = null;
        for(ParameterVectorUpdateable update : workerUpdates) {
            if(first == null)
                first = update;
            else
                first.get().addi(update.get());
        }

        first.get().divi(workerUpdates.size());
        lastMasterUpdate = first;
        return first;
    }



    @Override
    public ParameterVectorUpdateable getResults() {
        return this.lastMasterUpdate;
    }

    @Override
    public void setup(Configuration c) {

    }

    public static void main(String[] args) throws Exception {
        Master pmn = new Master();
        ApplicationMaster<ParameterVectorUpdateable> am = new ApplicationMaster<>(
                pmn, ParameterVectorUpdateable.class);

        ToolRunner.run(am, args);
    }

}