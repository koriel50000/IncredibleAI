# -*- coding: utf-8 -*-

from tensorflow import keras

# 定数宣言
input_rows = 8
input_cols = 8
input_channel = 16


# モデル定義
inputs = keras.layers.Input(shape=(input_rows * input_cols * input_channel))
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
predictions = keras.layers.Dense(1, activation='tanh')(x)
model = keras.Model(inputs=inputs, outputs=predictions)

model.compile(optimizer=keras.optimizers.Adam(1e-5),
              loss=keras.losses.MeanSquaredError(),
              metrics=['accuracy'])


#
# モデルを学習する
#
def training_model(datasets):
    x_train = datasets.train.images
    y_train = datasets.train.labels
    print("images;", x_train.shape)
    print("labels:", y_train.shape)
    model.summary()

    model.fit(x_train, y_train, batch_size=1, epochs=1)


#
# 予測値を計算する
#
def calculate_predicted_value(state):
    state_ = state.reshape(1, -1)
    predictions_ = model.predict(state_)
    return predictions_[0][0]


def save_checkpoint(file, step):
    model.save_weights(file + '-' + str(step))


def load_checkpoint(file, step):
    model.load_weights(file + '-' + str(step))


def save_model(file):
    model.save(file)
