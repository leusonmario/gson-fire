package io.gsonfire.gson;

import io.gsonfire.ClassConfig;
import io.gsonfire.PostProcessor;
import io.gsonfire.PreProcessor;
import io.gsonfire.TypeSelector;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

/**
 * @autor: julio
 */
public final class FireTypeAdapter<T> extends TypeAdapter<T> {

    private final Class<T> clazz;
    private final ClassConfig<? super T> classConfig;
    private final Gson gson;
    private final TypeAdapter<T> originalTypeAdapter;
    private final HooksInvoker hooksInvoker = new HooksInvoker();

    public FireTypeAdapter(Class<T> classAdapter, ClassConfig<? super T> classConfig, TypeAdapter<T> originalTypeAdapter, Gson gson) {
        this.classConfig = classConfig;
        this.gson = gson;
        this.originalTypeAdapter = originalTypeAdapter;
        this.clazz = classAdapter;
    }

    @Override
    public void write(JsonWriter out, T value) throws IOException {
        if(classConfig.isHooksEnabled()){
            hooksInvoker.preSerialize(value);
        }

        JsonElement res = originalTypeAdapter.toJsonTree(value);

        //Run all the post serializers
        runPostSerialize(res, value);

        gson.toJson(res, out);
    }

    @Override
    public T read(JsonReader in) throws IOException {
        JsonElement json = new JsonParser().parse(in);
        runPreDeserialize(json);
        T result = deserialize(json);

        //Run all the post deserializers
        if(classConfig.isHooksEnabled()){
            hooksInvoker.postDeserialize(result);
        }
        runPostDeserialize(result, json);

        return result;
    }

    private void runPostSerialize(JsonElement json, T src){
        for(PostProcessor<? super T> postProcessor: classConfig.getPostProcessors()){
            postProcessor.postSerialize(json, src, gson);
        }
    }

    private void runPostDeserialize(T res, JsonElement src){
        for(PostProcessor<? super T> postProcessor: classConfig.getPostProcessors()){
            postProcessor.postDeserialize(res, src, gson);
        }
    }

    private void runPreDeserialize(JsonElement json){
        for(PreProcessor<? super T> preProcessor: classConfig.getPreProcessors()){
            preProcessor.preDeserialize(clazz, json, gson);
        }
    }

    private T deserialize(JsonElement json){
        Class clazzDeserialize = null;

        //We only want to run the type selector is we are deserializing the base class for the type selector.
        if(clazz == classConfig.getConfiguredClass()){
            TypeSelector<? super T> selector = classConfig.getTypeSelector();
            if(selector != null){
                clazzDeserialize = classConfig.getTypeSelector().getClassForElement(json);
            }
        }
        T result;
        if(clazzDeserialize == null || clazzDeserialize == classConfig.getConfiguredClass()){
            result = originalTypeAdapter.fromJsonTree(json);
        } else {
            result = (T)gson.fromJson(json, clazzDeserialize);
        }
        return result;
    }
}
