package roboliq.hdf5

import java.nio.ByteBuffer
import java.io.ByteArrayOutputStream


class Hdf5Stream(os: ByteArrayOutputStream) {
	def putAscii(s: String, length: Int) {
		putBytesPlusNull(s.getBytes("ASCII"), length)
	}

	def putUtf8(s: String, length: Int) {
		// FIXME: this could lead to an error in which a multi-byte UTF-8 code gets truncated
		putBytesPlusNull(s.getBytes("UTF-8"), length)
	}
	
	private def putBytesPlusNull(l: Array[Byte], length: Int) {
		putBytes(l, length - 1)
		os.write(0)
	}
	
	def putBytes(l: Array[Byte], length: Int) {
		val n = math.min(length, l.length)
		os.write(l, 0, n)
		for (i <- n until length)
			os.write(0)
	}
	
	def putInt32(x: Int) {
		val buf = ByteBuffer.allocate(4)
		buf.order(java.nio.ByteOrder.LITTLE_ENDIAN)
		buf.putInt(x)
		putBytes(buf.array(), 4)
	}
}
