# -*- coding: utf-8 -*-

import tensorflow as tf
from tensorflow import keras

# メモリ使用を制限
# https://www.tensorflow.org/api_docs/python/tf/config/experimental/set_memory_growth
#physical_devices = tf.config.list_physical_devices('GPU')
#try:
#    tf.config.experimental.set_memory_growth(physical_devices[0], True)
#except:
    # Invalid device or cannot modify virtual devices once initialized.
#    pass


# 定数宣言
input_height = 8
input_width = 8
input_channels = 16


# モデル定義
inputs = keras.Input(shape=(input_height * input_width * input_channels,), name='inputs')
x = keras.layers.Reshape((input_height, input_width, input_channels))(inputs)
x = keras.layers.Conv2D(64, 5, padding='SAME', activation='relu', use_bias=False)(x)
x = keras.layers.Conv2D(64, 3, padding='SAME', activation='relu', use_bias=False)(x)
x = keras.layers.Conv2D(64, 3, padding='SAME', activation='relu', use_bias=False)(x)
x = keras.layers.Conv2D(64, 3, padding='SAME', activation='relu', use_bias=False)(x)
x = keras.layers.Conv2D(64, 3, padding='SAME', activation='relu', use_bias=False)(x)
x = keras.layers.Conv2D(1, 1, padding='SAME', activation='relu', use_bias=False)(x)
x = keras.layers.Flatten()(x)
x = keras.layers.Dense(256, activation='relu')(x)
outputs = keras.layers.Dense(1, activation='tanh', name='outputs')(x)
model = keras.Model(inputs=inputs, outputs=outputs, name='cnn_model')

model.compile(optimizer=keras.optimizers.Adam(1e-5),
              loss=keras.losses.MeanSquaredError(),
              metrics=['accuracy'])


#
# モデルを学習する
#
def training_model(dataset, checkpoint_prefix, batch_size, initial_epoch, epochs):
    dataset = dataset.shuffle(buffer_size=1024).batch(batch_size)

    model.summary()

    checkpoint_path = checkpoint_prefix + "_{epoch:02d}"
    checkpoint = keras.callbacks.ModelCheckpoint(checkpoint_path,
                                                 monitor='val_accuracy',
                                                 verbose=1,
                                                 save_weights_only=True)

    model.fit(dataset, batch_size=batch_size, initial_epoch=initial_epoch, epochs=epochs, callbacks=[checkpoint])


#
# 予測値を計算する
#
def calculate_predicted_values(states):
    return model.predict(states).reshape(-1).tolist()


#
# 予測値を計算する
#
def calculate_predicted_value(state):
    state_ = state.reshape(1, -1)
    predictions_ = model.predict(state_)
    return predictions_[0][0]


#
# 学習済みモデルを保存する
#
def save_model(export_dir):
    keras.models.save_model(model, export_dir)


#
# 学習済みモデルを読み込む
#
def load_model(import_dir):
    global model

    model = keras.models.load_model(import_dir)


#
# 学習データをロードする
#
def load_dataset(filename, parse_function):
    raw_dataset = tf.data.TFRecordDataset([filename], compression_type="GZIP")
    parsed_dataset = raw_dataset.map(parse_function)
    return parsed_dataset


#
# チェックポイントを読み込む
#
def load_checkpoint(checkpoint_prefix, epoch):
    checkpoint_path = checkpoint_prefix + "_{:02d}".format(epoch)
    model.load_weights(checkpoint_path)
