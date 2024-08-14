/*
 * Copyright (C) 2023 Authlete, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.authlete.cose;


import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.junit.jupiter.api.Test;
import com.authlete.cbor.CBORItem;
import com.authlete.cose.constants.COSEAlgorithms;
import com.authlete.cose.constants.COSEEllipticCurves;
import com.authlete.cose.constants.COSEKeyOperations;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.KeyOperation;
import com.nimbusds.jose.jwk.KeyType;
import com.nimbusds.jose.util.Base64URL;


public class COSESigningTest
{
    private static final ECPrivateKey EC_PRIVATE_KEY_11 = createECPrivateKey_11();
    private static final ECPublicKey  EC_PUBLIC_KEY_11  = createECPublicKey_11();


    private static byte[] fromHex(String hex)
    {
        try
        {
            return Hex.decodeHex(hex);
        }
        catch (DecoderException cause)
        {
            // This should not happen.
            cause.printStackTrace();
            return null;
        }
    }


    private static String toBase64Url(byte[] bytes)
    {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }


    private static ECPrivateKey createECPrivateKey_11()
    {
        try
        {
            return createPrivateCOSEEC2Key_11().toECPrivateKey();
        }
        catch (Exception cause)
        {
            // This should not happen.
            cause.printStackTrace();
            return null;
        }
    }


    private static COSEEC2Key createPrivateCOSEEC2Key_11()
    {
        // RFC 9052, C.7.2. Private Keys
        //
        // {
        //   1:2,
        //   2:'11',
        //   -1:1,
        //   -2:h'bac5b11cad8f99f9c72b05cf4b9e26d244dc189f745228255a219a86d6a09eff',
        //   -3:h'20138bf82dc1b6d562be0fa54ab7804a3a64b6d72ccfed6b6fb6ed28bbfc117e',
        //   -4:h'57c92077664146e876760c9520d054aa93c3afb04e306705db6090308507b4d3'
        // }

        return new COSEKeyBuilder()
                .ktyEC2()
                .kid("11")
                .ec2CrvP256()
                .ec2X(fromHex("bac5b11cad8f99f9c72b05cf4b9e26d244dc189f745228255a219a86d6a09eff"))
                .ec2Y(fromHex("20138bf82dc1b6d562be0fa54ab7804a3a64b6d72ccfed6b6fb6ed28bbfc117e"))
                .ec2D(fromHex("57c92077664146e876760c9520d054aa93c3afb04e306705db6090308507b4d3"))
                .buildEC2Key()
                ;
    }


    private static ECPublicKey createECPublicKey_11()
    {
        try
        {
            return createPublicCOSEEC2Key_11().toECPublicKey();
        }
        catch (Exception cause)
        {
            // This should not happen.
            cause.printStackTrace();
            return null;
        }
    }


    private static COSEEC2Key createPublicCOSEEC2Key_11()
    {
        // RFC 9052, C.7.1. Public Keys
        //
        // {
        //     -1:1,
        //     -2:h'bac5b11cad8f99f9c72b05cf4b9e26d244dc189f745228255a219a86d6a09eff',
        //     -3:h'20138bf82dc1b6d562be0fa54ab7804a3a64b6d72ccfed6b6fb6ed28bbfc117e',
        //     1:2,
        //     2:'11'
        // }

        return new COSEKeyBuilder()
                .ec2CrvP256()
                .ec2X(fromHex("bac5b11cad8f99f9c72b05cf4b9e26d244dc189f745228255a219a86d6a09eff"))
                .ec2Y(fromHex("20138bf82dc1b6d562be0fa54ab7804a3a64b6d72ccfed6b6fb6ed28bbfc117e"))
                .ktyEC2()
                .kid("11")
                .buildEC2Key()
                ;
    }


    private static JWK toJwk(Map<String, Object> map)
    {
        try
        {
            return JWK.parse(map);
        }
        catch (ParseException e)
        {
            e.printStackTrace();
            fail(e.getMessage());

            return null;
        }
    }


    @Test
    public void test_rfc9052_appendixC_1_1() throws COSEException
    {
        // https://www.rfc-editor.org/rfc/rfc9052.html#appendix-C.1.1
        //
        // 98(
        //   [
        //     / protected / h'',
        //     / unprotected / {},
        //     / payload / 'This is the content.',
        //     / signatures / [
        //       [
        //         / protected h'a10126' / << {
        //             / alg / 1:-7 / ECDSA 256 /
        //           } >>,
        //         / unprotected / {
        //           / kid / 4:'11'
        //         },
        //         / signature / h'e2aeafd40d69d19dfe6e52077c5d7ff4e408282cbefb
        // 5d06cbf414af2e19d982ac45ac98b8544c908b4507de1e90b717c3d34816fe926a2b
        // 98f53afd2fa0f30a'
        //       ]
        //     ]
        //   ]
        // )

        byte[] rawSignature = fromHex(
                "e2aeafd40d69d19dfe6e52077c5d7ff4e408282cbefb" +
                "5d06cbf414af2e19d982ac45ac98b8544c908b4507de1e90b717c3d34816fe926a2b" +
                "98f53afd2fa0f30a");

        COSESign sign = new COSESignBuilder()
                .payload("This is the content.")
                .signature(new COSESignature(
                        new COSEProtectedHeaderBuilder().alg(COSEAlgorithms.ES256).build(),
                        new COSEUnprotectedHeaderBuilder().kid("11").build(),
                        rawSignature
                ))
                .build();

        // The protected header should be empty.
        assertEquals(0, sign.getProtectedHeader().getParameters().size());

        // The unprotected header should be empty.
        assertEquals(0, sign.getUnprotectedHeader().getParameters().size());

        // The number of COSE signatures should be 1.
        List<? extends CBORItem> coseSignatures = sign.getSignatures().getItems();
        assertEquals(1, coseSignatures.size());

        // The first COSE signature in the COSESign instance.
        COSESignature coseSignature = (COSESignature)coseSignatures.get(0);

        // The 'signature' of the COSE signature should hold the raw signature.
        assertArrayEquals(rawSignature, coseSignature.getSignature().getValue());

        // Create a verifier.
        COSEVerifier verifier = new COSEVerifier(EC_PUBLIC_KEY_11);

        try
        {
            // Verify the COSESign object.
            boolean valid = verifier.verify(sign);

            assertTrue(valid, "Signature verification failed.");
        }
        catch (Exception cause)
        {
            cause.printStackTrace();
            fail("COSEVerifier threw an exception: " + cause.getMessage());
        }
    }


    @Test
    public void test_rfc9052_appendixC_2_1() throws COSEException
    {
        // https://www.rfc-editor.org/rfc/rfc9052.html#appendix-C.2.1
        //
        // 18(
        //   [
        //     / protected h'a10126' / << {
        //         / alg / 1:-7 / ECDSA 256 /
        //       } >>,
        //     / unprotected / {
        //       / kid / 4:'11'
        //     },
        //     / payload / 'This is the content.',
        //     / signature / h'8eb33e4ca31d1c465ab05aac34cc6b23d58fef5c083106c4
        // d25a91aef0b0117e2af9a291aa32e14ab834dc56ed2a223444547e01f11d3b0916e5
        // a4c345cacb36'
        //   ]
        // )

        byte[] rawSignature = fromHex(
                "8eb33e4ca31d1c465ab05aac34cc6b23d58fef5c083106c4" +
                "d25a91aef0b0117e2af9a291aa32e14ab834dc56ed2a223444547e01f11d3b0916e5" +
                "a4c345cacb36");

        COSESign1 sign1 = new COSESign1Builder()
                .protectedHeader(
                        new COSEProtectedHeaderBuilder().alg(COSEAlgorithms.ES256).build()
                )
                .unprotectedHeader(
                        new COSEUnprotectedHeaderBuilder().kid("11").build()
                )
                .payload("This is the content.")
                .signature(rawSignature)
                .build();

        // Create a verifier.
        COSEVerifier verifier = new COSEVerifier(EC_PUBLIC_KEY_11);

        try
        {
            // Verify the COSESign1 object.
            boolean valid = verifier.verify(sign1);

            assertTrue(valid, "Signature verification failed.");
        }
        catch (Exception cause)
        {
            cause.printStackTrace();
            fail("COSEVerifier threw an exception: " + cause.getMessage());
        }
    }


    @Test
    public void test_signing_01() throws COSEException
    {
        byte[] data  = "test".getBytes(StandardCharsets.UTF_8);
        byte[] data2 = "test2".getBytes(StandardCharsets.UTF_8);
        int    alg   = COSEAlgorithms.ES256;

        // Sign
        byte[] signature = COSESigner.sign(EC_PRIVATE_KEY_11, alg, data);

        // Verify; the result should be valid.
        boolean valid = COSEVerifier.verify(EC_PUBLIC_KEY_11, alg, data, signature);
        assertTrue(valid, "Signature verification failed.");

        // Verify; the result should be invalid.
        valid = COSEVerifier.verify(EC_PUBLIC_KEY_11, alg, data2, signature);
        assertFalse(valid, "Signature verification failed.");
    }


    @Test
    public void test_signing_02() throws COSEException
    {
        // Signature algorithm
        int algorithm = COSEAlgorithms.ES256;

        // Protected header
        COSEProtectedHeader protectedHeader =
                new COSEProtectedHeaderBuilder().alg(algorithm).build();

        // Unprotected header
        COSEUnprotectedHeader unprotectedHeader =
                new COSEUnprotectedHeaderBuilder().kid("11").build();

        // Payload
        String payload = "This is the content.";

        // Sig_structure
        SigStructure structure = new SigStructureBuilder()
                .signature1()
                .bodyAttributes(protectedHeader)
                .payload(payload)
                .build();

        // Signer
        COSESigner signer = new COSESigner(EC_PRIVATE_KEY_11);

        // Signature
        byte[] signature = signer.sign(structure, algorithm);

        // COSESign1
        COSESign1 sign1 = new COSESign1Builder()
                .protectedHeader(protectedHeader)
                .unprotectedHeader(unprotectedHeader)
                .payload(payload)
                .signature(signature)
                .build();

        // Verifier
        COSEVerifier verifier = new COSEVerifier(EC_PUBLIC_KEY_11);

        // Verification
        boolean valid = verifier.verify(sign1);

        assertTrue(valid, "Signature verification failed.");
    }

    @Test
    public void test_signing_eddsa_01() throws COSEException, NoSuchAlgorithmException
    {
        KeyPair keyPair = EdDSA.generateKeyPair();
        // Signature algorithm
        int algorithm = COSEAlgorithms.EdDSA;

        // Protected header
        COSEProtectedHeader protectedHeader =
                new COSEProtectedHeaderBuilder().alg(algorithm).build();

        // Unprotected header
        COSEUnprotectedHeader unprotectedHeader =
                new COSEUnprotectedHeaderBuilder().kid("11").build();

        // Payload
        String payload = "This is the content signed with EdDSA.";

        // Sig_structure
        SigStructure structure = new SigStructureBuilder()
                .signature1()
                .bodyAttributes(protectedHeader)
                .payload(payload)
                .build();

        // Signer
        COSESigner signer = new COSESigner(keyPair.getPrivate());

        // Signature
        byte[] signature = signer.sign(structure, algorithm);

        // COSESign1
        COSESign1 sign1 = new COSESign1Builder()
                .protectedHeader(protectedHeader)
                .unprotectedHeader(unprotectedHeader)
                .payload(payload)
                .signature(signature)
                .build();

        // Verifier
        COSEVerifier verifier = new COSEVerifier(keyPair.getPublic());

        // Verification
        boolean valid = verifier.verify(sign1);

        assertTrue(valid, "Signature verification failed.");
    }

    @Test
    public void test_ec2_private_jwk()
    {
        // Create a COSE key that represents an EC private key.
        COSEEC2Key coseKey = createPrivateCOSEEC2Key_11();

        assertEquals(true, coseKey.isPrivate());

        // Convert the COSE key into a Map that represents a JWK.
        Map<String, Object> map = coseKey.toJwk();

        String expectedKty = "EC";
        String expectedKid = "11";
        String expectedCrv = "P-256";
        byte[] d           = fromHex("57c92077664146e876760c9520d054aa93c3afb04e306705db6090308507b4d3");
        String expectedD   = toBase64Url(d);

        assertEquals(expectedKty, map.get("kty"));
        assertEquals(expectedKid, map.get("kid"));
        assertEquals(expectedCrv, map.get("crv"));
        assertEquals(expectedD,   map.get("d"));

        // Convert the map to a JWK.
        JWK jwk = toJwk(map);

        assertEquals(true, jwk.isPrivate());

        ECKey ecKey = jwk.toECKey();

        assertEquals(KeyType.EC,          ecKey.getKeyType());
        assertEquals(Curve.P_256,         ecKey.getCurve());
        assertEquals(Base64URL.encode(d), ecKey.getD());
    }


    @Test
    public void test_ec2_public_jwk()
    {
        // Create a COSE key that represents an EC public key.
        COSEEC2Key coseKey = createPublicCOSEEC2Key_11();

        assertEquals(false, coseKey.isPrivate());

        // Convert the COSE key into a Map that represents a JWK.
        Map<String, Object> map = coseKey.toJwk();

        String expectedKty = "EC";
        String expectedKid = "11";
        String expectedCrv = "P-256";
        byte[] x           = fromHex("bac5b11cad8f99f9c72b05cf4b9e26d244dc189f745228255a219a86d6a09eff");
        byte[] y           = fromHex("20138bf82dc1b6d562be0fa54ab7804a3a64b6d72ccfed6b6fb6ed28bbfc117e");
        String expectedX   = toBase64Url(x);
        String expectedY   = toBase64Url(y);

        assertEquals(expectedKty, map.get("kty"));
        assertEquals(expectedKid, map.get("kid"));
        assertEquals(expectedCrv, map.get("crv"));
        assertEquals(expectedX,   map.get("x"));
        assertEquals(expectedY,   map.get("y"));

        // Convert the map to a JWK.
        JWK jwk = toJwk(map);

        assertEquals(false, jwk.isPrivate());

        ECKey ecKey = jwk.toECKey();

        assertEquals(KeyType.EC,          ecKey.getKeyType());
        assertEquals(Curve.P_256,         ecKey.getCurve());
        assertEquals(Base64URL.encode(x), ecKey.getX());
        assertEquals(Base64URL.encode(y), ecKey.getY());
    }


    @Test
    public void test_cose_key_jwk() throws COSEException
    {
        COSEKey coseKey = new COSEKeyBuilder()
                .ktyEC2()
                .alg(COSEAlgorithms.ES256)
                .keyOps(Arrays.asList(COSEKeyOperations.VERIFY))
                .ec2Crv(COSEEllipticCurves.P_256)
                .ec2X(fromHex("bac5b11cad8f99f9c72b05cf4b9e26d244dc189f745228255a219a86d6a09eff"))
                .ec2Y(fromHex("20138bf82dc1b6d562be0fa54ab7804a3a64b6d72ccfed6b6fb6ed28bbfc117e"))
                .build();

        // Convert the COSE key into a Map that represents a JWK.
        Map<String, Object> map = coseKey.toJwk();

        String       expectedAlg    = "ES256";
        List<String> expectedKeyOps = Arrays.asList("verify");

        assertEquals(expectedAlg,    map.get("alg"));
        assertEquals(expectedKeyOps, map.get("key_ops"));

        // Convert the map to a JWK.
        JWK jwk = toJwk(map);

        Set<KeyOperation> expectedKeyOperations =
                new HashSet<>(Arrays.asList(KeyOperation.VERIFY));

        assertEquals(JWSAlgorithm.ES256,    jwk.getAlgorithm());
        assertEquals(expectedKeyOperations, jwk.getKeyOperations());
    }
}
