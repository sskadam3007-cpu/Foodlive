import boto3
import uuid
from django.conf import settings
from django.contrib.gis.geos import Point
from django.contrib.gis.measure import D
from rest_framework import viewsets, permissions, status
from rest_framework.decorators import action
from rest_framework.response import Response
from .models import MealPost, SpotRating
from .serializers import MealPostSerializer, SpotRatingSerializer

class MealPostViewSet(viewsets.ModelViewSet):
    queryset = MealPost.objects.all()
    serializer_class = MealPostSerializer
    permission_classes = [permissions.IsAuthenticatedOrReadOnly]

    def perform_create(self, serializer):
        serializer.save(user=self.request.user)

    @action(detail=False, methods=['get'])
    def nearby(self, request):
        lat = request.query_params.get('lat')
        lng = request.query_params.get('lng')
        radius_km = float(request.query_params.get('radius_km', 5.0))

        if not lat or not lng:
            return Response({"error": "lat and lng parameters are required"}, status=status.HTTP_400_BAD_REQUEST)

        user_location = Point(float(lng), float(lat), srid=4326)
        nearby_meals = MealPost.objects.filter(location__distance_lte=(user_location, D(km=radius_km)))
        serializer = self.get_serializer(nearby_meals, many=True)
        return Response(serializer.data)

    @action(detail=False, methods=['post'], permission_classes=[permissions.IsAuthenticated])
    def generate_upload_url(self, request):
        file_extension = request.data.get('extension', 'jpg')
        file_key = f"meals/{request.user.id}/{uuid.uuid4()}.{file_extension}"

        s3_client = boto3.client(
            's3',
            aws_access_key_id=settings.AWS_ACCESS_KEY_ID,
            aws_secret_access_key=settings.AWS_SECRET_ACCESS_KEY,
            region_name=settings.AWS_S3_REGION_NAME
        )

        presigned_data = s3_client.generate_presigned_post(
            Bucket=settings.AWS_STORAGE_BUCKET_NAME,
            Key=file_key,
            Fields={"acl": "public-read", "Content-Type": f"image/{file_extension}"},
            Conditions=[
                {"acl": "public-read"},
                {"Content-Type": f"image/{file_extension}"},
                ["content-length-range", 100, 10485760]  # Up to 10MB
            ],
            ExpiresIn=300
        )
        return Response(presigned_data)

class SpotRatingViewSet(viewsets.ModelViewSet):
    queryset = SpotRating.objects.all()
    serializer_class = SpotRatingSerializer
    permission_classes = [permissions.IsAuthenticated]

    def perform_create(self, serializer):
        serializer.save(user=self.request.user)