package roo.display.encoder;

import java.io.IOException;
import java.io.OutputStream;

class Rle8Encoder extends Encoder {
  private OutputStream os;
  private byte[] buffer;
  private int buffer_size;
  private int run_length;

  public Rle8Encoder(OutputStream os) {
    this.os = os;
    this.buffer = new byte[256];
    this.buffer_size = 0;
    this.run_length = 0;
  }

  public void encodePixel(int pixel) throws IOException {
    os.write((pixel >> 24) & 0xFF); // a
    os.write((pixel >> 16) & 0xFF); // r
    os.write((pixel >> 8) & 0xFF);  // g
    os.write(pixel & 0xFF);         // b
  }

  private void encodeIndex(int index) throws IOException {
    int buffer_position = buffer_size++;
    buffer[buffer_position] = (byte)index;
    if (buffer_size == 1) {
      // First pixel in a new series; not clear yet which encoding to use.
      run_length = 1;
    } else if (buffer[buffer_position] == buffer[buffer_position = 1]) {
      run_length++;
      // If we had a bunch of distinct pixels, and then a run length of at least
      // 5, it pays to emit
    } else {
      run_length = 1;
    }
  }

  public void close() {}

  private static class CircularBuffer {
    private byte[] buffer;
    private int capacity;
    private int head;
    private int size;

    public CircularBuffer() {
      capacity = 256;
      buffer = new byte[capacity];
      head = 0;
      size = 0;
    }
    
    public void pushTail(byte data) {
      if (size >= capacity) {
        throw new RuntimeException("Overflow");
      }
      buffer[(head + size) % capacity] = data;
      ++size;
    }

    public int size() { return size; }

    // public byte get(int index) {
    //   if (index < 0 || index >= size) {
    //     throw new IllegalArgumentExeption(index);
    //   }
    //   int offset = (head + index) % capacity;
    //   return buffer[index];
    // }

    public byte popHead() {
      if (size == 0) {
        throw new RuntimeException();
      }
      byte result = buffer[head++];
      head %= capacity;
      return result;
    }

    public void dropHead(int count) {
      if (count > size) {
        throw new IllegalArgumentException(
            "Buffer does not have sufficient elements: " + count);
      }
      head += count;
      head %= capacity;
    }
  }
}

// 1 2 3 4 5 5 5 6 7 8

//     0 5 1 2 3 4 5 2 5 0 3 6 7 8

//     0 4 1 2 3 4 3 5 0 3 6 7 8

//     0 10 1 2 3 4 5 5 5 6 7 8