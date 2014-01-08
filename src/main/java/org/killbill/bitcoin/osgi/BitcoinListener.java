/*
 * Copyright 2010-2013 Ning, Inc.
 *
 * Ning licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.killbill.bitcoin.osgi;

import com.google.bitcoin.core.AbstractWalletEventListener;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.Wallet;
import com.google.bitcoin.kits.WalletAppKit;
import com.google.bitcoin.params.MainNetParams;
import com.google.bitcoin.params.RegTestParams;
import com.google.bitcoin.params.TestNet3Params;
import com.google.bitcoin.utils.BriefLogFormatter;
import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class BitcoinListener {

    private static final Logger log = LoggerFactory.getLogger(BitcoinListener.class);

    private final WalletAppKit kit;
    private final TransactionManager transactionManager;
    private final BitcoinConfig config;

    private volatile boolean isInitialized;

    public BitcoinListener(final TransactionManager transactionManager, final BitcoinConfig config) {
        this.transactionManager = transactionManager;
        this.config = config;
        this.kit = initializeKit();
        this.isInitialized = false;
    }


    public void initialize() {
        // Download the block chain and wait until it's done.
        kit.startAndWait();

        addKeyIfMissing();

        kit.wallet().addEventListener(new AbstractWalletEventListener() {
            @Override
            public void onTransactionConfidenceChanged(Wallet wallet, Transaction tx) {

                log.info("Bitcoin listener received new transaction " + tx.getHash() + ", confidence = " + tx.getConfidence());

                if (tx.getConfidence().getDepthInBlocks() < config.getConfidenceBlockDepth()) {
                    return;
                }
                if (transactionManager.isPendingTransaction(tx.getHash())) {
                    log.info("Bitcoin notifing transaction manager for " + tx.getHash() + ", confidence = " + tx.getConfidence());
                    transactionManager.notifyPaymentSystem(tx.getHash());
                }
            }
        });
        this.isInitialized = true;
    }

    public void addKeyIfMissing() {
        if (config.shouldGenerateKey()) {
            final ECKey newKey = new ECKey();
            kit.wallet().addKey(newKey);
            log.info("GENERATED NEW KEY FOR BITCOIN WALLET : " + newKey.toAddress(getNetworkParameters()));
        }
    }

    private WalletAppKit initializeKit() {

        BriefLogFormatter.init();

        final NetworkParameters params = getNetworkParameters();
        final String filePrefix = getFilePrefix();

        // Start up a basic app using a class that automates some boilerplate.
        final WalletAppKit tmpKit = new WalletAppKit(params, new File(config.getInstallDirectory()), filePrefix);

        if (params == RegTestParams.get()) {
            // Regression test mode is designed for testing and development only, so there's no public network for it.
            // If you pick this mode, you're expected to be running a local "bitcoind -regtest" instance.
            tmpKit.connectToLocalHost();
        }
        return tmpKit;
    }

    @VisibleForTesting
    WalletAppKit getKit() {
        return kit;
    }

    @VisibleForTesting
    NetworkParameters getNetworkParameters() {
        NetworkParameters params;
        if (config.getNetworkName().equals("testnet")) {
            params = TestNet3Params.get();
        } else if (config.getNetworkName().equals("regtest")) {
            params = RegTestParams.get();
        } else {
            params = MainNetParams.get();
        }
        return params;
    }

    private String getFilePrefix() {
        if (config.getNetworkName().equals("testnet")) {
            return "killbill-bitcoin-testnet";
        } else if (config.getNetworkName().equals("regtest")) {
            return "killbill-bitcoin-regtest";
        } else {
            return "killbill-bitcoin";
        }
    }
}
