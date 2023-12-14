package com.noor.essex.urgo

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso

class SellImageAdapter(
    private val images: List<Uri>
): RecyclerView.Adapter<SellImageAdapter.ImageViewHolder>() {

    inner class ImageViewHolder(view: View): RecyclerView.ViewHolder(view) {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.image_rv_item, parent, false
        )
        return ImageViewHolder(view)
    }

    override fun getItemCount(): Int {
        return images.size
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val item = images[position]

        val imageView = holder.itemView.findViewById<ImageView>(R.id.image)

        println("FATAL:: calling onBindViewholder.. ")
        Picasso.get().load(item).placeholder(R.drawable.img_bg_form_anuncio)
            .into(imageView)
    }
}