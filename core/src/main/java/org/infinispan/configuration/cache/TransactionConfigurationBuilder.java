package org.infinispan.configuration.cache;

import org.infinispan.transaction.LockingMode;
import org.infinispan.transaction.TransactionMode;
import org.infinispan.transaction.lookup.GenericTransactionManagerLookup;
import org.infinispan.transaction.lookup.TransactionManagerLookup;
import org.infinispan.transaction.lookup.TransactionSynchronizationRegistryLookup;
import org.infinispan.transaction.tm.BatchModeTransactionManager;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

import java.util.concurrent.TimeUnit;

public class TransactionConfigurationBuilder extends AbstractConfigurationChildBuilder<TransactionConfiguration> {

   private static Log log = LogFactory.getLog(TransactionConfigurationBuilder.class);

   private boolean autoCommit = true;
   private long cacheStopTimeout = TimeUnit.SECONDS.toMillis(30);
   private boolean eagerLockingSingleNode = false;
   LockingMode lockingMode = LockingMode.OPTIMISTIC;
   private boolean syncCommitPhase = true;
   private boolean syncRollbackPhase = false;
   private TransactionManagerLookup transactionManagerLookup;
   private TransactionSynchronizationRegistryLookup transactionSynchronizationRegistryLookup;
   TransactionMode transactionMode = null;
   private boolean useEagerLocking = false;
   private boolean useSynchronization = false;
   private final RecoveryConfigurationBuilder recovery;
   private boolean use1PcForAutoCommitTransactions = false;

   TransactionConfigurationBuilder(ConfigurationBuilder builder) {
      super(builder);
      this.recovery = new RecoveryConfigurationBuilder(this);
   }
   
   public TransactionConfigurationBuilder autoCommit(boolean b) {
      this.autoCommit = b;
      return this;
   }

   @Deprecated
   public TransactionConfigurationBuilder cacheStopTimeout(int i) {
      this.cacheStopTimeout = i;
      return this;
   }
   
   public TransactionConfigurationBuilder cacheStopTimeout(long l) {
      this.cacheStopTimeout = l;
      return this;
   }

   public TransactionConfigurationBuilder eagerLockingSingleNode(boolean b) {
      this.eagerLockingSingleNode = b;
      return this;
   }

   public TransactionConfigurationBuilder lockingMode(LockingMode lockingMode) {
      this.lockingMode = lockingMode;
      return this;
   }

   public TransactionConfigurationBuilder syncCommitPhase(boolean b) {
      this.syncCommitPhase = b;
      return this;
   }

   public TransactionConfigurationBuilder syncRollbackPhase(boolean b) {
      this.syncRollbackPhase = b;
      return this;
   }

   public TransactionConfigurationBuilder transactionManagerLookup(TransactionManagerLookup tml) {
      this.transactionManagerLookup = tml;
      return this;
   }
   
   public TransactionConfigurationBuilder transactionSynchronizationRegistryLookup(TransactionSynchronizationRegistryLookup lookup) {
      this.transactionSynchronizationRegistryLookup = lookup;
      return this;
   }

   public TransactionConfigurationBuilder transactionMode(TransactionMode transactionMode) {
      this.transactionMode = transactionMode;
      return this;
   }

   public TransactionConfigurationBuilder useEagerLocking(boolean b) {
      this.useEagerLocking = b;
      return this;
   }

   public TransactionConfigurationBuilder useSynchronization(boolean b) {
      this.useSynchronization = b;
      return this;
   }
   
   public RecoveryConfigurationBuilder recovery() {
      recovery.enable();
      return recovery;
   }

   public TransactionConfigurationBuilder use1PcForAutoCommitTransactions(boolean b) {
      this.use1PcForAutoCommitTransactions = b;
      return this;
   }

   @Override
   void validate() {
      if (transactionManagerLookup == null) {
         if (!getBuilder().invocationBatching().enabled) {
            transactionManagerLookup = new GenericTransactionManagerLookup();
         } else {
            if (!useSynchronization) log.debug("Switching to Synchronization based enlistment.");
            useSynchronization = true;
         }
      }
   }

   @Override
   TransactionConfiguration create() {
      if (useEagerLocking) {
         lockingMode = LockingMode.PESSIMISTIC;
      }
      if (transactionMode == null && getBuilder().invocationBatching().enabled)
         transactionMode = TransactionMode.TRANSACTIONAL;
      else if (transactionMode == null)
         transactionMode = TransactionMode.NON_TRANSACTIONAL;
      return new TransactionConfiguration(autoCommit, cacheStopTimeout, eagerLockingSingleNode, lockingMode, syncCommitPhase, syncRollbackPhase, transactionManagerLookup, transactionSynchronizationRegistryLookup, transactionMode, useEagerLocking, useSynchronization, use1PcForAutoCommitTransactions, recovery.create());
   }
   
   @Override
   public TransactionConfigurationBuilder read(TransactionConfiguration template) {
      this.autoCommit = template.autoCommit();
      this.cacheStopTimeout = template.cacheStopTimeout();
      this.eagerLockingSingleNode = template.eagerLockingSingleNode();
      this.lockingMode = template.lockingMode();
      this.syncCommitPhase = template.syncCommitPhase();
      this.syncRollbackPhase = template.syncRollbackPhase();
      this.transactionManagerLookup = template.transactionManagerLookup();
      this.transactionMode = template.transactionMode();
      this.transactionSynchronizationRegistryLookup = template.transactionSynchronizationRegistryLookup();
      this.useEagerLocking = template.useEagerLocking();
      this.useSynchronization = template.useSynchronization();
      this.use1PcForAutoCommitTransactions = template.use1PcForAutoCommitTransactions();
      this.recovery.read(template.recovery());
      
      return this;
   }
}
