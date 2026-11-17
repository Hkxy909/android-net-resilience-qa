package com.hkxy.netqa;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class AdbNetworkController {

	public ArrayList<String> runCommand(String[] cmd) {
		ArrayList<String> lines = new ArrayList<>();
		try {
			ProcessBuilder pb = new ProcessBuilder(cmd);
			pb.redirectErrorStream(true);
			Process proc = pb.start();

			BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			String line = reader.readLine();
			while (line != null) {
				lines.add(line.trim());
				line = reader.readLine();
			}
			reader.close();
			proc.waitFor();
		} catch (Exception e) {
			return new ArrayList<>();
		}
		return lines;
	}

	public boolean isDeviceConnected() {
		ArrayList<String> out = runCommand(new String[] { "adb", "devices" });
		for (int i = 0; i < out.size(); i++) {
			String line = out.get(i);
			if (!line.startsWith("List of devices") && line.endsWith("\tdevice")) {
				return true;
			}
		}
		return false;
	}

	public boolean setWifiState(boolean enable) {
		String state = enable ? "enable" : "disable";
		ArrayList<String> out = runCommand(new String[] { "adb", "shell", "svc", "wifi", state });
		return out != null;
	}

	public boolean setAirplaneMode(boolean enable) {
		String state = enable ? "enable" : "disable";
		ArrayList<String> out = runCommand(new String[] { "adb", "shell", "cmd", "connectivity", "airplane-mode", state });
		return out != null;
	}
}
