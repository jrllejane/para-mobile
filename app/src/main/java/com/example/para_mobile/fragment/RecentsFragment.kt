package com.example.para_mobile.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.para_mobile.R
import com.example.para_mobile.adapter.RecentLocationAdapter
import com.example.para_mobile.model.LocationResult
import com.google.gson.Gson

class RecentsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: RecentLocationAdapter
    private var recentLocations: MutableList<LocationResult> = mutableListOf()

    private var onRecentLocationListener: ((String) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_recents, container, false)

        recyclerView = view.findViewById(R.id.recent_locations_recyclerview)
        recyclerView.layoutManager = LinearLayoutManager(context)

        adapter = RecentLocationAdapter(recentLocations) { selected ->
            onRecentLocationListener?.invoke(selected.display_name)
        }

        recyclerView.adapter = adapter

        loadRecentLocations()

        return view
    }

    fun setOnRecentLocationListener(listener: (String) -> Unit) {
        onRecentLocationListener = listener
    }

    private fun saveRecentLocation(location: LocationResult) {
        val prefs = requireContext().getSharedPreferences("recents", Context.MODE_PRIVATE)
        val editor = prefs.edit()

        val gson = Gson()
        val recentSet = prefs.getStringSet("locations", mutableSetOf())?.toMutableSet() ?: mutableSetOf()

        val json = gson.toJson(location)
        recentSet.removeIf { it.contains(location.display_name) } // prevent duplicates
        recentSet.add(json)

        // Optional: limit to 3 or 5 most recent
        val trimmed = recentSet.toList().takeLast(5).toSet()

        editor.putStringSet("locations", trimmed)
        editor.apply()
    }

    // Optional: expose this to allow external update (e.g., after new location is added)
    fun updateRecents(newList: List<LocationResult>) {
        recentLocations.clear()
        recentLocations.addAll(newList)
        adapter.notifyDataSetChanged()
    }

    private fun loadRecentLocations() {
        val prefs = requireContext().getSharedPreferences("recents", Context.MODE_PRIVATE)
        val gson = Gson()

        val recentSet = prefs.getStringSet("locations", emptySet())?.toList()?.reversed() ?: listOf()

        val loadedLocations = recentSet.mapNotNull { json ->
            try {
                gson.fromJson(json, LocationResult::class.java)
            } catch (e: Exception) {
                null // skip malformed entries
            }
        }

        recentLocations.clear()
        recentLocations.addAll(loadedLocations)
        adapter.notifyDataSetChanged()
    }

    fun refreshRecents() {
        loadRecentLocations()
    }


}
