Author: 
-------
Antonio Santiago <asantiagop@gmail.com>

Description:
------------
This file describes the changes needed to allow modifiable opacity in some 
layers, those that use the SurfaceTileRenderer class to render its tiles.

The class SurfaceTileRenderer is responsible to render tiles of any tiled layer,
the problem is it doesn't take in account the layer opacity.
The methods responsibles to render tiles are 'renderTile' and 'renderTiles'.
The idea is to add a reference on these methods to the layer that contains the 
tiles to be drawn and add the  code to set the opacity in the tile rendering 
process.


Changes from original WWJ 0.3.0:
--------------------------------

I have attached the modified files, but here are a brief description of changes 
respect WWJ 0.3.0. Note that almost all changes are in SurfaceTileRenderer class.

* 'SurfaceTileRenderer.java'. On 'rederTiles' method, added a reference
  to the layer that contains the tiles. Then every tile can be rendered with
  the opacity specified in the layer:

  Change:
            public void renderTiles(DrawContext dc, Iterable<? extends SurfaceTile> tiles) ...
  by:
            public void renderTiles(DrawContext dc, Iterable<? extends SurfaceTile> tiles, AbstractLayer layer)...

  After next code:

            ...
            if (!dc.isPickingMode()) {
                gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE, GL.GL_REPLACE);
            } else {
                gl.glTexEnvf(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE, GL.GL_COMBINE);
                gl.glTexEnvf(GL.GL_TEXTURE_ENV, GL.GL_SRC0_RGB, GL.GL_PREVIOUS);
                gl.glTexEnvf(GL.GL_TEXTURE_ENV, GL.GL_COMBINE_RGB, GL.GL_REPLACE);
            }

  add the next code to set the opacity:

            // Set opacity
            if (layer != null) {
                gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE, GL.GL_MODULATE);
                gl.glColor4f(1, 1, 1, (float) layer.getOpacity());
            }
            ...

  Also modify the 'renderTile' method adding a new parameter that reference the
  owner layer. Change:
            public void renderTile(DrawContext dc, SurfaceTile tile) ...
  by:
            public void renderTile(DrawContext dc, SurfaceTile tile, AbstractLayer layer) ...
  
  and modify the line:
            this.renderTiles(dc, al);
  by:
            this.renderTiles(dc, al, layer);

  Finally, you need to overload both methods 'renderTiles' and 'renderTile':

            public void renderTile(DrawContext dc, SurfaceTile tile) {
                this.renderTile(dc, tile, null);
            }

            public void renderTiles(DrawContext dc, Iterable<? extends SurfaceTile> tiles) {
                this.renderTiles(dc, tiles, null);
            }


* Finally, you must change the invokation to the above methods in two clases,
  TileImageLayer and RPFLayer.

  In TileImageLayer's 'draw' method you must change the call:

            dc.getGeographicSurfaceTileRenderer().renderTiles(dc, this.currentTiles);
  by:
            dc.getGeographicSurfaceTileRenderer().renderTiles(dc, this.currentTiles, this);

  And in RPFLayer change lines:
            
            dc.getGeographicSurfaceTileRenderer().renderTile(dc, tile);  
            ...
            dc.getGeographicSurfaceTileRenderer().renderTiles(dc, rpfTextureTiles);
  by:
            dc.getGeographicSurfaceTileRenderer().renderTile(dc, tile, this);
            ...
            dc.getGeographicSurfaceTileRenderer().renderTiles(dc, rpfTextureTiles, this);

Note these changes not alter the opacity in some Renderable objects, like SurfaceImage
or SurfaceShape. These classes use SurfaceTileRenderer to render itself but
there is no easy way to pass a reference to the owner layer without modifying
the Renderable interface and many code.
