package android.net;

import android.os.Parcel;

import java.util.Collections;
import java.util.List;

/**
 * Minimal concrete {@link Uri} for JVM unit tests. The android.jar Uri can't be created off-device
 * ({@code Uri.parse} isn't implemented), but its constructor is package-private, so a test class in
 * this package can extend it.
 */
public final class FakeUri extends Uri {
    private final String value;

    public FakeUri(String value) {
        this.value = value;
    }

    @Override public Builder buildUpon() { throw new UnsupportedOperationException(); }
    @Override public String getAuthority() { return null; }
    @Override public String getEncodedAuthority() { return null; }
    @Override public String getEncodedFragment() { return null; }
    @Override public String getEncodedPath() { return null; }
    @Override public String getEncodedQuery() { return null; }
    @Override public String getEncodedSchemeSpecificPart() { return value; }
    @Override public String getEncodedUserInfo() { return null; }
    @Override public String getFragment() { return null; }
    @Override public String getHost() { return null; }
    @Override public String getLastPathSegment() { return null; }
    @Override public String getPath() { return null; }
    @Override public List<String> getPathSegments() { return Collections.emptyList(); }
    @Override public int getPort() { return -1; }
    @Override public String getQuery() { return null; }
    @Override public String getScheme() { return null; }
    @Override public String getSchemeSpecificPart() { return value; }
    @Override public String getUserInfo() { return null; }
    @Override public boolean isHierarchical() { return false; }
    @Override public boolean isRelative() { return true; }
    @Override public String toString() { return value; }
    @Override public boolean equals(Object other) { return other instanceof FakeUri && ((FakeUri) other).value.equals(value); }
    @Override public int hashCode() { return value.hashCode(); }
    @Override public int describeContents() { return 0; }
    @Override public void writeToParcel(Parcel dest, int flags) { throw new UnsupportedOperationException(); }
}
