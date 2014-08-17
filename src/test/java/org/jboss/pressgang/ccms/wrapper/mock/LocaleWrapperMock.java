package org.jboss.pressgang.ccms.wrapper.mock;

import org.apache.commons.lang.math.RandomUtils;
import org.jboss.pressgang.ccms.wrapper.LocaleWrapper;

public class LocaleWrapperMock implements LocaleWrapper {
    private Integer id = RandomUtils.nextInt();
    private String value;
    private String translationValue;
    private String buildValue;

    public LocaleWrapperMock(final String value, final String translationValue, final String buildValue) {
        this.value = value;
        this.translationValue = translationValue;
        this.buildValue = buildValue;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String getTranslationValue() {
        return translationValue;
    }

    @Override
    public void setTranslationValue(String translationValue) {
        this.translationValue = translationValue;
    }

    @Override
    public String getBuildValue() {
        return buildValue;
    }

    @Override
    public void setBuildValue(String buildValue) {
        this.buildValue = buildValue;
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    @Override
    public LocaleWrapper clone(boolean deepCopy) {
        // TODO
        return null;
    }

    @Override
    public Object unwrap() {
        // TODO
        return null;
    }
}
