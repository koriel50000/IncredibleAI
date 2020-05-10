# -*- coding: utf-8 -*-

import datasets
from tensorflow import keras

# 定数宣言
input_rows = 8
input_cols = 8
input_channel = 16


# モデル定義
inputs = keras.Input(shape=(input_rows * input_cols * input_channel,), name='inputs')
x = keras.layers.Reshape((input_rows, input_cols, input_channel))(inputs)
x = keras.layers.Conv2D(64, 5, padding='SAME', activation='relu', use_bias=False)(x)
x = keras.layers.Conv2D(64, 3, padding='SAME', activation='relu', use_bias=False)(x)
x = keras.layers.Conv2D(64, 3, padding='SAME', activation='relu', use_bias=False)(x)
x = keras.layers.Conv2D(64, 3, padding='SAME', activation='relu', use_bias=False)(x)
x = keras.layers.Conv2D(64, 3, padding='SAME', activation='relu', use_bias=False)(x)
x = keras.layers.Conv2D(1, 1, padding='SAME', activation='relu', use_bias=False)(x)
x = keras.layers.Flatten()(x)
x = keras.layers.Dense(256, activation='relu')(x)
x = keras.layers.Dropout(0.01)(x)
outputs = keras.layers.Dense(1, activation='tanh', name='outputs')(x)
model = keras.Model(inputs=inputs, outputs=outputs, name='cnn_model')

model.compile(optimizer=keras.optimizers.Adam(1e-5),
              loss=keras.losses.MeanSquaredError(),
              metrics=['accuracy'])


#
# モデルを学習する
#
def training_model(datasets_dir, checkpoint_dir, step, epoch, epoch_size):
    prefix = 'train1k{:02d}'.format(step)
    reversi_data = datasets.read_data_sets(datasets_dir, prefix)
    x_train = reversi_data.train.images
    y_train = reversi_data.train.labels
    print("images;", x_train.shape)
    print("labels:", y_train.shape)
    model.summary()

    checkpoint_path = checkpoint_dir + prefix + '_{epoch:02d}'
    checkpoint = keras.callbacks.ModelCheckpoint(checkpoint_path,
                                                 save_weights_only=True)

    model.fit(x_train, y_train, batch_size=1, initial_epoch=epoch, epochs=epoch_size, callbacks=[checkpoint])


#
# 予測値を計算する
#
def calculate_predicted_value(state):
    state_ = state.reshape(1, -1)
    predictions_ = model.predict(state_)
    return predictions_[0][0]


def save_model(export_dir):
    keras.models.save_model(model, export_dir)


def load_checkpoint(checkpoint_dir, step, epoch):
    filename = 'train1k{:02d}_{:02d}'.format(step, epoch)
    model.load_weights(checkpoint_dir + filename)
