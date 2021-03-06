// Copyright (C) 2011-2012 CRS4.
//
// This file is part of Seal.
//
// Seal is free software: you can redistribute it and/or modify it
// under the terms of the GNU General Public License as published by the Free
// Software Foundation, either version 3 of the License, or (at your option)
// any later version.
//
// Seal is distributed in the hope that it will be useful, but
// WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
// or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
// for more details.
//
// You should have received a copy of the GNU General Public License along
// with Seal.  If not, see <http://www.gnu.org/licenses/>.


package tests.it.crs4.seal.recab;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;

import org.junit.*;
import static org.junit.Assert.*;

import it.crs4.seal.recab.PackedSequence;

public class TestPackedSequence
{
	private PackedSequence seq;
	private File dataFile;
	private ByteBuffer space;

	@Before
	public void setup() throws java.io.IOException
	{
		seq = new PackedSequence();
		dataFile = File.createTempFile("packed_sequence_test", null);
		space = ByteBuffer.allocate(10);
	}

	@After
	public void tearDown() throws java.io.IOException
	{
		seq.close();
		dataFile.delete();
	}

	@Test
	public void testSimple() throws java.io.IOException
	{
		// Write a packed sequence to a file.  Re-read it and test for equality.
		// AGCT    = 0 2 1 3  =
		//           00100111
		//           0x2 0x7
		byte[] packedBytes = { 0x27 };
		writeBytesToDataFile(packedBytes);
		seq.load(dataFile);
		seq.readSection(space, 0, 4);
		compareBytes(new byte[]{0, 2, 1, 3}, space);
	}

	@Test
	public void testPart() throws java.io.IOException
	{
		// Write a packed sequence that takes part of a byte to a file.
		// Re-read it and test for equality.
		//     TCG =  3 1 2 =
		//            11011000
		//            0xD 0x8
		byte[] packedBytes = { (byte)0xD8 };
		writeBytesToDataFile(packedBytes);
		seq.load(dataFile);
		seq.readSection(space, 0, 3);
		compareBytes(new byte[]{3, 1, 2}, space);
	}

	@Test
	public void testOneAndABit() throws java.io.IOException
	{
		// Write a packed sequence that takes one byte plus part of a byte to a file.
		// Re-read it and test for equality.
		// AGCTTCG = 0 2 1 3 | 3 1 2 =
		//           00100111| 11011000
		//           0x2 0x7 | 0xD 0x8
		byte[] packedBytes = { 0x27, (byte)0xD8 };
		writeBytesToDataFile(packedBytes);
		seq.load(dataFile);
		seq.readSection(space, 0, 7);
		compareBytes(new byte[]{ 0, 2, 1, 3, 3, 1, 2 }, space);
	}

	@Test
	public void testReadMiddleSection() throws java.io.IOException
	{
		// Read middle part of a packed sequence.
		// Re-read it and test for equality.
		// AGCTTCG = 0 2 1 3 | 3 1 2 =
		//           00100111| 11011000
		//           0x2 0x7 | 0xD 0x8
		// Read CTT - start 2, len 3
		byte[] packedBytes = { 0x27, (byte)0xD8 };
		writeBytesToDataFile(packedBytes);
		seq.load(dataFile);
		seq.readSection(space, 2, 3);
		compareBytes(new byte[]{ 1, 3, 3 }, space);
	}

	@Test
	public void testReadBwaPac() throws java.io.IOException
	{
		String sequence = "AATAACTAAAGTTAGCTGCCCTGGACTATTCACCCCCTAGTCTCAATTTAAGAAGATCC";
		// These are the bytes of the packed sequence generated by bwa 0.5.8c for the sequence above
		byte[] bwaData = {
			(byte)0x0c, (byte)0x1c, (byte)0x0b, (byte)0xc9, (byte)0xe5, (byte)0x7a,
			(byte)0x1c, (byte)0xf4, (byte)0x55, (byte)0x72, (byte)0xdd, (byte)0x0f,
			(byte)0xc2, (byte)0x08, (byte)0xd4, (byte)0x03};

		space = ByteBuffer.allocate(sequence.length()); // the default one is too small

		writeBytesToDataFile(bwaData);
		seq.load(dataFile);
		seq.readSection(space, 0, sequence.length());
		space.position(0);

		String retrieved = PackedSequence.bytesToBases(space);
		assertEquals(sequence, retrieved);
	}

	private boolean compareBytes(byte[] array, ByteBuffer buffer)
	{
		for (int i = 0; i < array.length; ++i)
		{
			assertEquals(array[i], buffer.get(i));
		}
		return true;
	}

	private void writeBytesToDataFile(byte[] bytes) throws java.io.IOException
	{
		FileOutputStream output = new FileOutputStream(dataFile);
		output.write(bytes);
		output.close();
	}
}
