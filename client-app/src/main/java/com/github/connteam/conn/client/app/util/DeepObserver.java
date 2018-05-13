package com.github.connteam.conn.client.app.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javafx.beans.property.Property;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

public class DeepObserver<T> {
    private final ObservableValue<? extends T> target;
    private final ObserveInitializer<? super T> setup;
    private final ChangeListener<T> mainListener = this::onChange;
    private List<Runnable> unbinders = new ArrayList<Runnable>();
    private boolean enabled = false;

    @FunctionalInterface
    public interface ObserveInitializer<T> {
        void init(DeepObserver<? extends T>.ObserverContext ctx, T old, T cur);
    }

    public DeepObserver(ObservableValue<? extends T> target, ObserveInitializer<? super T> setup) {
        this.target = target;
        this.setup = setup;
    }

    public static <T> DeepObserver<T> listen(ObservableValue<? extends T> target, ObserveInitializer<? super T> setup) {
        DeepObserver<T> observer = new DeepObserver<>(target, setup);
        observer.enable();
        return observer;
    }

    public ObservableValue<? extends T> getObserved() {
        return target;
    }

    public void enable() {
        if (enabled) {
            return;
        }

        enabled = true;
        onChange(target, null, target.getValue());
        target.addListener(mainListener);
    }

    public void disable() {
        if (!enabled) {
            return;
        }

        enabled = false;
        target.removeListener(mainListener);
        onChange(target, target.getValue(), null);
    }

    private void unbind() {
        for (Runnable x : unbinders) {
            x.run();
        }
        unbinders.clear();
    }

    public void onChange(ObservableValue<? extends T> prop, T old, T cur) {
        if (old != cur) {
            unbind();
            setup.init(new ObserverContext(), old, cur);
        }
    }

    public class ObserverContext {
        public <S> void bind(Property<S> left, ObservableValue<? extends S> right) {
            left.bind(right);
            unbinders.add(() -> left.unbind());
        }

        public <S> void bindBidirectional(Property<S> left, Property<S> right) {
            left.bindBidirectional(right);
            unbinders.add(() -> left.unbindBidirectional(right));
        }

        public <S> void deepListen(ObservableValue<S> prop, ObserveInitializer<? super S> setup) {
            DeepObserver<S> observer = DeepObserver.listen(prop, setup);
            unbinders.add(() -> observer.disable());
        }

        public <S> void listen(ObservableValue<S> prop, ChangeListener<? super S> listener) {
            if (prop.getValue() != null) {
                listener.changed(prop, null, prop.getValue());
            }

            prop.addListener(listener);

            unbinders.add(() -> {
                prop.removeListener(listener);

                if (prop.getValue() != null) {
                    listener.changed(prop, prop.getValue(), null);
                }
            });
        }

        public <S> void listen(ObservableList<S> prop, ListChangeListener<? super S> listener) {
            if (!prop.isEmpty()) {
                listener.onChanged(new ListChangeListener.Change<S>(prop) {
                    int stage = 0;

                    @Override
                    public boolean next() {
                        return ++stage < 2;
                    }

                    @Override
                    public void reset() {
                        stage = 0;
                    }

                    @Override
                    public int getFrom() {
                        return 0;
                    }

                    @Override
                    public int getTo() {
                        return (stage == 1 ? prop.size() : 0);
                    }

                    @Override
                    public List<S> getRemoved() {
                        return Collections.emptyList();
                    }

                    @Override
                    protected int[] getPermutation() {
                        return new int[0];
                    }
                });
            }

            prop.addListener(listener);

            unbinders.add(() -> {
                prop.removeListener(listener);

                if (!prop.isEmpty()) {
                    listener.onChanged(new ListChangeListener.Change<S>(FXCollections.emptyObservableList()) {
                        int stage = 0;

                        @Override
                        public boolean next() {
                            return ++stage < 2;
                        }

                        @Override
                        public void reset() {
                            stage = 0;
                        }

                        @Override
                        public int getFrom() {
                            return 0;
                        }

                        @Override
                        public int getTo() {
                            return 0;
                        }

                        @Override
                        public List<S> getRemoved() {
                            return Collections.unmodifiableList(getList());
                        }

                        @Override
                        protected int[] getPermutation() {
                            return new int[0];
                        }
                    });
                }
            });
        }
    }
}
