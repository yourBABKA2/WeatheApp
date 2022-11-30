package com.example.weatherapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.weatherapp.R
import com.example.weatherapp.databinding.ItemListBinding
import com.squareup.picasso.Picasso

class WeatherAdapter(val listener: Listener?) : ListAdapter<WeatherModel, WeatherAdapter.Holder>(Comparator()) {
    class Holder(view: View, val listener: Listener?) :
        RecyclerView.ViewHolder(view) { //Создание класса ViewHolder, который хранит в себе ссылки на элементы шаблона разметки
        val binding = ItemListBinding.bind(view)
        var itemTemp: WeatherModel? = null
        init{
            itemView.setOnClickListener{
                itemTemp?.let { it1 -> listener?.itemClick(it1) }
            }
        }

        fun bind(item: WeatherModel) = with(binding) { //Функция заполнения данными
            itemTemp = item
            textDate.text = item.time
            textConditions.text = item.condition
            textTemp.text = item.currentTemp.ifEmpty { "${item.maxTemp}°C/${item.minTemp}°C" }
            Picasso.get().load("https:" + item.imgUrl).into(image)
        }
    }

    //Сравнение элементов, если не совпадает, проиходит удалние элемента
    class Comparator : DiffUtil.ItemCallback<WeatherModel>() {
        override fun areItemsTheSame(oldItem: WeatherModel, newItem: WeatherModel): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: WeatherModel, newItem: WeatherModel): Boolean {
            return oldItem == newItem
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_list, parent, false)
        return Holder(view, listener)
    }

    //Отрисовка на экране
    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(getItem(position))
    }

    interface Listener{
        fun itemClick(item: WeatherModel)
    }
}