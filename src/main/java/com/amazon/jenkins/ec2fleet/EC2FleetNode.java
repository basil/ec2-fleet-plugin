package com.amazon.jenkins.ec2fleet;

import hudson.Extension;
import hudson.model.Computer;
import hudson.model.Descriptor;
import hudson.model.Node;
import hudson.model.Slave;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.EphemeralNode;
import hudson.slaves.NodeProperty;
import hudson.slaves.RetentionStrategy;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.util.List;

public class EC2FleetNode extends Slave implements EphemeralNode, EC2FleetCloudAware {

    private volatile AbstractEC2FleetCloud cloud;
    private int maxTotalUses;

    public EC2FleetNode(final String name, final String nodeDescription, final String remoteFS, final int numExecutors, final Mode mode, final String label,
                        final List<? extends NodeProperty<?>> nodeProperties, final AbstractEC2FleetCloud cloud, ComputerLauncher launcher, final int maxTotalUses) throws IOException, Descriptor.FormException {
        //noinspection deprecation
        super(name, nodeDescription, remoteFS, numExecutors, mode, label,
                launcher, RetentionStrategy.NOOP, nodeProperties);
        this.cloud = cloud;
        this.maxTotalUses = maxTotalUses;
    }

    @Override
    public Node asNode() {
        return this;
    }

    @Override
    public String getDisplayName() {
        // in some multi-thread edge cases cloud could be null for some time, just be ok with that
        return (cloud == null ? "unknown fleet" : cloud.getDisplayName()) + " " + name;
    }

    @Override
    public Computer createComputer() {
        return new EC2FleetNodeComputer(this, name, cloud);
    }

    @Override
    public AbstractEC2FleetCloud getCloud() {
        return cloud;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setCloud(@NonNull AbstractEC2FleetCloud cloud) {
        this.cloud = cloud;
    }

    public int getMaxTotalUses() {
        return this.maxTotalUses;
    }

    public void setMaxTotalUses(final int maxTotalUses) {
        this.maxTotalUses = maxTotalUses;
    }

    @Extension
    public static final class DescriptorImpl extends SlaveDescriptor {
        public DescriptorImpl() {
            super();
        }

        public String getDisplayName() {
            return "Fleet Slave";
        }

        /**
         * We only create this kind of nodes programmatically.
         */
        @Override
        public boolean isInstantiable() {
            return false;
        }
    }
}
