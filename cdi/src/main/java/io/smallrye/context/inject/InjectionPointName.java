package io.smallrye.context.inject;

/**
 * Helper class to store the name defined by @NamedInstance as well as the name would be used via MP Config
 */
public class InjectionPointName {

    private String namedInstanceName;
    private String mpConfigName;

    public InjectionPointName(String namedInstanceName, String mpConfigName) {
        if (namedInstanceName == null) {
            throw new IllegalArgumentException("Parameter namedInstance of InjectionPointName must be non-null");
        }
        this.mpConfigName = mpConfigName;
        this.namedInstanceName = namedInstanceName;
    }

    public InjectionPointName(String namedInstanceName) {
        this(namedInstanceName, null);
    }

    public String getNamedInstanceName() {
        return namedInstanceName;
    }

    public String getMpConfigName() {
        return mpConfigName;
    }

    @Override
    public int hashCode() {
        return (this.namedInstanceName.hashCode()) / 5;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof InjectionPointName)) {
            return false;
        }
        InjectionPointName other = (InjectionPointName) o;
        // we are deliberately only comparing based on namedInstanceName as MP Config name changes for each IP
        // whereas this one can be shared
        return this.namedInstanceName.equals(((InjectionPointName) o).namedInstanceName);
    }
}