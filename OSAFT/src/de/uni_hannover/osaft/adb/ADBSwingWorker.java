/*******************************************************************************
 * Copyright (c) 2013 Jannis Koenig.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Jannis Koenig - initial API and implementation
 ******************************************************************************/
package de.uni_hannover.osaft.adb;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import de.uni_hannover.osaft.Main;
import de.uni_hannover.osaft.plugininterfaces.ViewPlugin;
import de.uni_hannover.osaft.view.OSAFTView;

/**
 * This class extends the {@link SwingWorker} and processes adb commands in his
 * {@link doInBackground} method
 * 
 * @author Jannis Koenig
 * 
 */
public class ADBSwingWorker extends SwingWorker<String, Object> {

	private String[] commands;
	private String adbExecutable, currentDevice;
	private Runtime rt;
	private OSAFTView view;
	private ViewPlugin plugin;
	private int exitCode;
	private boolean showProgressBar;
	private static final Logger log = Logger.getLogger(ADBSwingWorker.class.getName());

	public ADBSwingWorker(String[] commands, String adbExecutable, String currentDevice, OSAFTView view, ViewPlugin plugin,
			boolean showProgressBar) {
		log.addHandler(Main.fh);
		this.commands = commands;
		this.view = view;
		this.adbExecutable = adbExecutable;
		this.plugin = plugin;
		this.currentDevice = currentDevice;
		this.showProgressBar = showProgressBar;
		rt = Runtime.getRuntime();
	}

	/**
	 * This method distinguishes between interaction with shell and single adb
	 * commands and processes them in background
	 */
	@Override
	protected String doInBackground() {
		String output = "";
		try {
			// show progressbar if command takes some time
			if (showProgressBar) {
				SwingUtilities.invokeLater(new Runnable() {

					@Override
					public void run() {
						view.getProgressDialog().setVisible(true);
						view.getProgressDialog().setTitle("Executing ADB command " + "\"" + commands[0] + "\"");
					}
				});
			}
			// interaction with shell:
			if (commands.length > 1) {
				String line;
				// start the adb shell with the Runtime on the chosen device
				Process p = rt.exec(adbExecutable + " -s " + currentDevice + " shell");

				// bufferout is the input for the shell, so all commands are
				// written to this stream
				BufferedOutputStream bufferout = new BufferedOutputStream(p.getOutputStream());
				PrintWriter commandInput = new PrintWriter((new OutputStreamWriter(bufferout)), true);
				for (int i = 0; i < commands.length; i++) {
					commandInput.println(commands[i]);
				}
				commandInput.close();
				bufferout.close();

				// the InputStream of the process p is the output on the console
				Reader r = new InputStreamReader(p.getInputStream());
				BufferedReader in = new BufferedReader(r);

				// clearance of the InputStream is mandatory to finish the
				// process:
				while ((line = in.readLine()) != null) {
					output += line + "\n";
				}
				in.close();
				r.close();
				// blocks the thread until process is finished
				exitCode = p.waitFor();
			}
			// only one command:
			else {
				// execute the single command on the chosen device
				Process p = rt.exec(adbExecutable + " -s " + currentDevice + " " + commands[0]);

				Reader r = new InputStreamReader(p.getInputStream());
				BufferedReader in = new BufferedReader(r);
				String line;
				// clearance of the InputStream is mandatory to finish the
				// process:
				while ((line = in.readLine()) != null) {
					output += line + "\n";
				}
				in.close();
				r.close();
				// blocks the thread until process is finished
				exitCode = p.waitFor();
			}
		} catch (IOException e) {
			log.log(Level.WARNING, e.toString(), e);
		} catch (InterruptedException e) {
			log.log(Level.WARNING, e.toString(), e);
		}
		return output;
	}

	/**
	 * When the commands are processed, this method calls the {@link
	 * reactToADBResult()}-method of the given {@link ViewPlugin} with the
	 * result of {@link doInBackground()}
	 */
	@Override
	public void done() {
		if (showProgressBar) {
			view.getProgressDialog().setVisible(false);
		}
		try {
			plugin.reactToADBResult(get(), commands);
		} catch (InterruptedException e) {
			log.log(Level.WARNING, e.toString(), e);
		} catch (ExecutionException e) {
			log.log(Level.WARNING, e.toString(), e);
		}
	}

	/**
	 * Returns the exit code of the executed command(s)
	 */
	public int getExitCode() {
		return exitCode;
	}

}
