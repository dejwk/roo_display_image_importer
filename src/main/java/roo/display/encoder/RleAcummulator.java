package roo.display.encoder;

public class RleAcummulator {
  //public interface Consumer { void addRun(int value, int count); }

  public class Entry {
    public final int value;
    public final int count;

    public Entry(int value, int count) {
      this.value = value;
      this.count = count;
    }

    public boolean equals(Object other) {
      if (!(other instanceof Entry))
        return false;
      Entry otherEntry = (Entry)other;
      return otherEntry.value == value && otherEntry.count == count;
    }

    public int hashCode() { return value * 371 + count; }
  }

  int currentValue;
  int currentCount;
  //final Consumer consumer;

  public RleAcummulator() {
    // this.consumer = consumer;
    this.currentValue = 0;
    this.currentCount = 0;
  }

  public Entry add(int value) {
    if (currentCount == 0) {
      currentValue = value;
      currentCount = 1;
      return null;
    } else if (currentValue == value) {
      ++currentCount;
      return null;
    } else {
      Entry result = new Entry(currentValue, currentCount);
      currentValue = value;
      currentCount = 1;
      return result;
    }
  }

  public Entry close() {
    if (currentCount > 0) {
      Entry result = new Entry(currentValue, currentCount);
      currentCount = 0;
      return result;
    } else {
      return null;
    }
  }
}
