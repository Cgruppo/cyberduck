/*
 *  SSHTools - Java SSH2 API
 *
 *  Copyright (C) 2002-2003 Lee David Painter and Contributors.
 *
 *  Contributions made by:
 *
 *  Brett Smith
 *  Richard Pernavas
 *  Erwin Bolwidt
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Library General Public License
 *  as published by the Free Software Foundation; either version 2 of
 *  the License, or (at your option) any later version.
 *
 *  You may also distribute it and/or modify it under the terms of the
 *  Apache style J2SSH Software License. A copy of which should have
 *  been provided with the distribution.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  License document supplied with your distribution for more details.
 *
 */
package com.sshtools.j2ssh.transport.cipher;

import java.io.InputStream;
import java.net.URL;
import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.sshtools.j2ssh.configuration.ConfigurationLoader;
import com.sshtools.j2ssh.io.IOUtil;
import com.sshtools.j2ssh.transport.AlgorithmNotSupportedException;
import com.sshtools.ext.bouncycastle.cipher.*;

/**
 * @author $author$
 * @version $Revision$
 */
public class SshCipherFactory {
	private static HashMap ciphers;
	private static String defaultCipher;
	private static Log log = LogFactory.getLog(SshCipherFactory.class);
	private static ArrayList supported;

	static {
		ciphers = new HashMap();

		log.info("Loading supported cipher algorithms");
		
		ciphers.put("3des-cbc", TripleDesCbc.class);
		ciphers.put("blowfish-cbc", BlowfishCbc.class);
		ciphers.put("twofish256-cbc", Twofish256Cbc.class);
		ciphers.put("twofish192-cbc", Twofish192Cbc.class);
		ciphers.put("twofish128-cbc", Twofish128Cbc.class);
		ciphers.put("aes256-cbc", AES256Cbc.class);
		ciphers.put("aes192-cbc", AES192Cbc.class);
		ciphers.put("aes128-cbc", AES128Cbc.class);
		ciphers.put("cast128-cbc", CAST128Cbc.class);
		
		defaultCipher = "blowfish-cbc";

		// Build a list of the supported ciphers
		supported = new ArrayList(ciphers.keySet());
	}

	/**
	 * Creates a new SshCipherFactory object.
	 */
	protected SshCipherFactory() {
	}

	/**
	 *
	 */
	public static void initialize() {
	}

	/**
	 * @return
	 */
	public static String getDefaultCipher() {
		return defaultCipher;
	}

	/**
	 * @return
	 */
	public static List getSupportedCiphers() {
		// Return the list
		return supported;
	}

	/**
	 * @param algorithmName
	 * @return
	 * @throws AlgorithmNotSupportedException
	 */
	public static SshCipher newInstance(String algorithmName)
	    throws AlgorithmNotSupportedException {
		log.info("Creating new "+algorithmName+" cipher instance");

		try {
			return (SshCipher)((Class)ciphers.get(algorithmName)).newInstance();
		}
		catch(Throwable t) {
			throw new AlgorithmNotSupportedException(algorithmName+
			    " is not supported!");
		}
	}
}
