package ru.netology.yandexmaps.fragments

import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.layers.GeoObjectTapListener
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.MapObjectTapListener
import com.yandex.mapkit.mapview.MapView
import com.yandex.runtime.ui_view.ViewProvider
import ru.netology.yandexmaps.R
import ru.netology.yandexmaps.databinding.FragmentMapBinding
import ru.netology.yandexmaps.viewmodel.MapPointsViewModel
import java.util.*


class MapFragment : Fragment() {
    private val viewModel: MapPointsViewModel by viewModels(
        ownerProducer = ::requireParentFragment
    )

    private lateinit var mapviewElement: MapView

    //Свойство добавлено, чтобы спасти Listener от сборщика мусора
    private lateinit var geoObjectTapListener: GeoObjectTapListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        val properties = Properties()
        val am = requireContext().assets
        val inputStream = am.open("maps.properties")
        properties.load(inputStream)

        val myApiKey = properties.getProperty("MAPS_API_KEY")
            ?: throw RuntimeException("Can't get API_KEY for maps")
        MapKitFactory.setApiKey(myApiKey)
        MapKitFactory.initialize(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentMapBinding.inflate(
            inflater,
            container,
            false
        )
        with(binding) {
            mapviewElement = mapview
            mapview.map.move(
                CameraPosition(viewModel.currentPosition, 11.0f, 0.0f, 0.0f),
                Animation(Animation.Type.SMOOTH, 0f),
                null
            )

            viewModel.data.observe(viewLifecycleOwner) { points ->
                mapview.map?.mapObjects?.clear()

                points.forEach { point ->
                    val view: View = View.inflate(requireContext(), R.layout.point_on_map, null)
                    val textView = view.findViewById<TextView>(R.id.pointDescription)
                    textView.text = point.text

                    val mapObjectTapListener = MapObjectTapListener { _, _ ->
                        viewModel.selectedPoint = point
                        viewModel.currentPosition = point.point
                        findNavController().navigate(R.id.action_mapFragment_to_addPointFragment)
                        true
                    }

                    mapview.map?.mapObjects?.addPlacemark(point.point, ViewProvider(view)).also {
                        it?.addTapListener(mapObjectTapListener)
                    }
                }
            }

            geoObjectTapListener = GeoObjectTapListener { geoObjectTapEvent ->
                viewModel.selectedPoint = null
                val currentPosition = viewModel.currentPosition
                viewModel.currentPosition =
                    geoObjectTapEvent.geoObject.geometry.getOrNull(0)?.point ?: currentPosition
                findNavController().navigate(R.id.action_mapFragment_to_addPointFragment)
                true
            }

            mapview.map?.addTapListener(geoObjectTapListener)
        }
        return binding.root
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.addPoint -> {
                viewModel.selectedPoint = null
                findNavController().navigate(R.id.action_mapFragment_to_addPointFragment)
                true
            }
            R.id.showPoints -> {
                findNavController().navigate(R.id.action_mapFragment_to_showPointsFragment)
                true
            }
            R.id.showLicenses -> {
                findNavController().navigate(R.id.action_mapFragment_to_licenseFragment)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onStop() {
        super.onStop()
        mapviewElement.onStop()
        MapKitFactory.getInstance().onStop()
    }

    override fun onStart() {
        super.onStart()
        mapviewElement.onStart()
        MapKitFactory.getInstance().onStart()
    }
}