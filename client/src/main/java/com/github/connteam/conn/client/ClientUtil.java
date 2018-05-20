package com.github.connteam.conn.client;

import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.Signature;
import java.security.SignatureException;
import java.util.zip.CRC32;

import com.github.connteam.conn.client.database.model.EphemeralKey;
import com.github.connteam.conn.core.crypto.CryptoUtil;
import com.github.connteam.conn.core.net.NetMessages;
import com.github.connteam.conn.core.net.proto.PeerProtos.PeerMessage;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ClientUtil {
    private final static Logger LOG = LoggerFactory.getLogger(ClientUtil.class);

    private ClientUtil() {
    }

    public static byte[] makeLoginSignature(KeyPair keyPair, String username, byte[] toSign)
            throws InvalidKeyException, SignatureException {
        Signature sign = CryptoUtil.newSignature(keyPair.getPrivate());
        sign.update(username.getBytes());
        sign.update(keyPair.getPublic().getEncoded());
        sign.update(toSign);
        return sign.sign();
    }

    public static EphemeralKey generateEphemeralKey() {
        KeyPair pair = CryptoUtil.generateKeyPair();
        EphemeralKey key = new EphemeralKey();
        key.setPrivateKey(pair.getPrivate());
        key.setPublicKey(pair.getPublic());
        return key;
    }

    public static long getPeerMessageChecksum(int id, byte[] encoded, byte[] padding) {
        CRC32 crc = new CRC32();
        crc.update(id);
        crc.update(padding);
        crc.update(encoded);
        return crc.getValue();
    }

    public static byte[] encodePeerMessage(Message msg) {
        int id = NetMessages.PEER.getID(msg);
        byte[] encoded = msg.toByteArray();

        int paddingSize = 20 + CryptoUtil.randomInt(100);
        byte[] padding = CryptoUtil.randomBytes(paddingSize);

        PeerMessage.Builder frame = PeerMessage.newBuilder();
        frame.setChecksum(getPeerMessageChecksum(id, encoded, padding));
        frame.setPadding(ByteString.copyFrom(padding));
        frame.setId(id);
        frame.setMessage(ByteString.copyFrom(encoded));

        return frame.build().toByteArray();
    }

    public static Message decodePeerMessage(byte[] data) {
        try {
            PeerMessage msg = PeerMessage.parseFrom(data);

            int id = msg.getId();
            byte[] encoded = msg.getMessage().toByteArray();
            byte[] padding = msg.getPadding().toByteArray();

            if (!NetMessages.PEER.containsID(id)) {
                LOG.warn("Received invalid peer message ID");
                return null;
            }

            if (getPeerMessageChecksum(id, encoded, padding) != msg.getChecksum()) {
                LOG.warn("Invalid peer message checksum");
                return null;
            }

            return NetMessages.PEER.getParser(id).parseFrom(encoded);
        } catch (InvalidProtocolBufferException e) {
            LOG.warn("Received invalid peer message", e);
            return null;
        }
    }
}
