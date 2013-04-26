package no.group09.stk500_v1;

import java.util.ArrayList;

public class Hex {
	private Logger logger;
	private ArrayList<ArrayList<Byte>> binList = new ArrayList<ArrayList<Byte>>();
	
	private byte[] subHex; 
	
	private int line = -1;
	private boolean state = false;
	
	public Hex(byte[] bin, Logger log) {
		this.logger = log;
		this.subHex = bin;
		
		// count lines and create an array
		state = splitHex();
		
		logger.logcat("Hex file status: " + state, "v");
		logger.logcat("Hex file has " + getLines() + " lines", "v");
	}
	
	/**
	 * Return a line from hex.
	 * @param line
	 * @return array with size, address (high), address (low) and data
	 */
	public byte[] getHexLine(int line)
	{
		return formatHexLine(line);
	}
	
	/**
	 * Return the load address for this line
	 * @param line The line number to check
	 * @return The load address, high byte, low byte
	 */
	public byte[] getLoadAddress(int line) {
		byte[] tempArray = new byte[2];
		tempArray[0] = binList.get(line).get(1);
		tempArray[1] = binList.get(line).get(2);
		return tempArray;
	}
	
	/**
	 * Returns number of lines in hex file.
	 * @return line from hex file
	 */
	public int getLines()
	{
		if(state) {
			return line;
		}
		else {
			return -1;
		}
	}
	
	/**
	 * 
	 * @param line number
	 * @return number of data bytes on line as unsigned integer
	 */
	public int getDataSizeOnLine(int line)
	{
		if(line<this.line) {
			logger.logcat("splitHex: Line " + line +
					" has " + (binList.get(line).get(0) & 0xFF) + " bytes","d");
			return binList.get(line).get(0) & 0xFF;
		}
		return -1;
	}
	
	/**
	 * Return state of hex file
	 * @return true if the hex file is correct
	 */
	public boolean getChecksumStatus()
	{
		return state;
	}
	
	/**
	 * Check if a byte array is exactly the same as the data fields in a specific line.
	 * If data contains more than 16 bytes, it will start on next line.
	 * 
	 * @param line to check
	 * @param data to check
	 * 
	 * @return true if the data array is equal to the data array in this hex file
	 */
	public boolean checkBytesOnLine(int line, byte[] data) {
		//logger.logcat("splitHex: Input: " + bytesToHex(data), "d");
		
		//Return false if the line does not exist
		if(line > this.line) {
			return false;
		}
		//Return true if data length is zero
		else if(data.length == 0 || data == null) {
			return true;
		}
		
		//Remember to ignore size, address x 2, record and checksum,
		//total 6 bytes
		for (int i = 0; i < binList.get(line).size()-6; i++) {
			
			//Last data byte, start on new line
			if(i == binList.get(line).size()-5) {
				logger.logcat("splitHex: Compare next line (" + line + ")", "d");
				
				byte tempData[] = new byte[data.length - binList.get(line).size()+5];
				for (int j = 0; j < tempData.length; j++) {
					tempData[j] = data[j+binList.get(line).size()-5];
				}
				return checkBytesOnLine(line+1, tempData);
			}
			else if(data[i] != binList.get(line).get(i+4)) {
				logger.logcat("splitHex: Input: " + bytesToHex(data), "d");
				
				logger.logcat("splitHex: Hex file: " + bytesToHex(formatHexLine(line)), "d");
				
				logger.logcat("splitHex: Compared " + oneByteToHex(data[i]) + " with " +
						oneByteToHex(binList.get(line).get(i+4)) +
						" on line " + line + " failed! Data number " + i, "w");
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Split the hex input into an array and check if it's correct.
	 * Each line must start with ':' (colon), byte value 58. The following
	 * values is 1 byte size, 2 byte address, n byte data, 1 byte checksum.
	 * The record is not saved, only used to check what kind of data this is.
	 * This will start with line 0 and parse through the whole file.
	 * 
	 * @return True if the hex file is correct.
	 */
	private boolean splitHex() {
		logger.logcat("splitHex: Number of bytes in array " + subHex.length, "d");
		int x = 0;
		while(x < subHex.length) {
			x = splitHex(x);
			
//			logger.logcat("splitHex: X = " + x, "d");
			
			if(x < 0) return false;
		}
		
		return true;
	}
	
	/**
	 * Split the hex input into an array and check if it's correct.
	 * Each line must start with ':' (colon), byte value 58. The following
	 * values is 1 byte size, 2 byte address, n byte data, 1 byte checksum.
	 * The record is not saved, only used to check what kind of data this is.
	 * 
	 * @param startOnDataByte Start reading at this byte number. When finished
	 * with this line, starting on the next line.
	 * 
	 * @return True if the hex file is correct.
	 */
	private int splitHex(int startOnDataByte) {
		int dataLength = 0;
		
		//The minimum length of a line is 6, including the start byte ':'
		if((subHex.length + startOnDataByte)<6) {
			logger.logcat("splitHex(): The minimum size of a line is 6, this line was " 
					+ subHex.length, "w");
			return -1;
		}
		
		//save length
		dataLength = subHex[startOnDataByte + 1];
		
		//The line must start with ':'
		if(subHex[startOnDataByte] != 58) {
			logger.logcat("splitHex(): Line not starting with ':' !", "w");
			return -1;
		}
		//If record type is 0x01 (file end) and data size > 0, return false
		else if(subHex[startOnDataByte + 4]==1 && dataLength>0) {
			logger.logcat("splitHex(): Contains data, but are told to stop!", "w");
			return -1;
		}
		//If record type is 0x01 (file end) and it exist more bytes to read, return false
		else if(subHex[startOnDataByte + 4]==1 && subHex.length>startOnDataByte + dataLength + 6) {
			logger.logcat("splitHex(): Contains more lines with data, " +
					"but are told to stop!", "w");
			return -1;
		}
		//If record type is 0x00 (data record) and data size equals 0, return false
		else if(subHex[startOnDataByte + 4]==0 && subHex[startOnDataByte + 1]==0) {
			logger.logcat("splitHex(): Told to send data, but contains no data!", "w");
			return -1;
		}
		else {
			//add new line to ArrayList
			line++;
			
//			logger.logcat("splitHex: creating line " + line + " startOnDataByte " + startOnDataByte, "v");
			
			binList.add(new ArrayList<Byte>());
			
			//Save data size
			binList.get(line).add(subHex[startOnDataByte + 1]); // size
			binList.get(line).add(subHex[startOnDataByte + 2]); // start address
			binList.get(line).add(subHex[startOnDataByte + 3]); // start address
			binList.get(line).add(subHex[startOnDataByte + 4]); // record
			
			//save data
			for (int i = startOnDataByte + 5; i < startOnDataByte + dataLength + 5; i++) {
				binList.get(line).add(subHex[i]);
			}
			
			//save checksum
			binList.get(line).add(subHex[startOnDataByte + 5 + dataLength]);
			
			byte[] t = new byte[binList.get(line).size()];
			for (int i = 0; i < binList.get(line).size(); i++) {
				t[i] = binList.get(line).get(i);
			}
			
			//Check if the checksum is correct
			if(checkData(line)) {
				//End of hex file
				try {
					// No more data on line
					return (startOnDataByte + dataLength + 6);
				} catch (ArrayIndexOutOfBoundsException e) {
					// Array out of bounds
					return -1;
				}
			}
			else {
				logger.logcat("splitHex(): Checksum failed!", "w");
				return -1;
			}
		}
	}
	
	
	/**
	 * Format line in hex file into an array with:
	 * 1 byte size
	 * 2 byte address
	 * n byte data
	 * @param line number in hex file
	 * @return byte array, empty if the line is out of bounds
	 */
	private byte[] formatHexLine(int line)
	{		
		byte tempBinary[] = null;
		
		//Check if the line is out of bounds
		try {
			//Create a new temporary array
			tempBinary = new byte[binList.get(line).size()-2];
						
			//Add elements into an array
			for(int i=0; i<binList.get(line).size()-1; i++) {
				//Ignore the record element, but save size and address
				if(i<3) {
					tempBinary[i] = binList.get(line).get(i);
				}
				//Ignore checksum
				else if(i>3) {
					tempBinary[i-1] = binList.get(line).get(i);
				}
			}
			return tempBinary;
		} catch (IndexOutOfBoundsException e) {
//			logger.logcat("formatHexLine(): Out of bounds!", "e");
			tempBinary = null;
			return tempBinary;
		}
	}
	
	/**
	 * Calculate and check the checksum of the given line in the binary array.
	 * 
	 * @param line integer telling which line in the binary array that is to be
	 * checksummed.
	 * 
	 * @return true if checksum is correct, false if not.
	 */
	private boolean checkData (int line) {
		//length of data
		int length = binList.get(line).size();

		int byteValue = 0;

		//Add the values of all the fields together, except checksum 
		for(int i=0; i<length-1; i++) {
			byteValue += binList.get(line).get(i);
		}

		int b = 0x100;

		byte check = (byte) (b-byteValue);
		
		return (check&0xFF) == (binList.get(line).get(length-1)&0xFF);
	}
	
	/**
	 * Convert a byte array into hex
	 * @param bytes
	 * @return string with hex
	 */
	public static String bytesToHex(byte[] bytes) {
		final char[] hexArray = {'0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f'};
		char[] hexChars = new char[bytes.length * 5];
		int v;
		for ( int j = 0; j < bytes.length; j++ ) {
			v = bytes[j] & 0xFF;
			hexChars[j * 5] = 91;
			hexChars[j * 5 + 1] = hexArray[v >>> 4];
			hexChars[j * 5 + 2] = hexArray[v & 0x0F];
			hexChars[j * 5 + 3] = 93;
			hexChars[j * 5 + 4] = 32;
		}
		return new String(hexChars);
	}
	
	/**
	 * Convert a byte into hex
	 * @param b
	 * @return string with one hex
	 */
	public static String oneByteToHex(byte b) {
		byte[] tempB = new byte[1];
		tempB[0] = b;
		return new String(bytesToHex(tempB));
	}
}
